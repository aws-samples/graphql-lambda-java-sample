//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package software.amazonaws.sample;

import lombok.Builder;
import lombok.Getter;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.iam.*;
import software.amazon.awscdk.services.rds.InstanceProps;
import software.amazon.awscdk.services.rds.*;
import software.amazon.awscdk.services.secretsmanager.Secret;
import software.amazon.awscdk.services.secretsmanager.SecretProps;
import software.amazon.awscdk.services.secretsmanager.SecretStringGenerator;
import software.constructs.Construct;

import java.util.Arrays;

@Builder
public class CommonStackUtil {

    private Construct scope;
    private Vpc vpc;
    private String logicalNamePrefix;
    private String physicalNamePrefix;
    private String dbUserName;
    private String dbName;
    private String dbPort;
    private String account;
    private String region;

    @Getter
    private DatabaseProxy databaseProxy;

    @Getter
    private Role lambdaRole;

    @Getter
    private SecurityGroup lambdaSG;

    private Secret getRDSSecret() {
        Secret rdsSecret = new Secret(this.scope, this.logicalNamePrefix + "RDSPassword", SecretProps.builder()
                .secretName(physicalNamePrefix + "-rds-cred")
                .generateSecretString(SecretStringGenerator.builder()
                        .excludePunctuation(true)
                        .passwordLength(16)
                        .generateStringKey("password")
                        .secretStringTemplate("{\"username\": \"" + dbUserName + "\"}")
                        .build())
                .build());
        return rdsSecret;
    }

    private SecurityGroup getRDSSecurityGroup() {
        SecurityGroup rdsSecurityGroup = new SecurityGroup(this.scope, this.logicalNamePrefix + "RDSSG", SecurityGroupProps.builder()
                .allowAllOutbound(true)
                .securityGroupName(this.physicalNamePrefix + "-rds-sg")
                .vpc(this.vpc)
                .build());
        return rdsSecurityGroup;
    }

    private DatabaseCluster getRDSCluster(Secret secret, SecurityGroup rdsSecurityGroup) {
        DatabaseCluster rdsCluster = new DatabaseCluster(this.scope, this.logicalNamePrefix + "RDSCluster", DatabaseClusterProps.builder()
                .engine(DatabaseClusterEngine.AURORA_MYSQL)
                .credentials(Credentials.fromSecret(secret))
                .instances(1)
                .defaultDatabaseName(dbName)
                .storageEncrypted(true)
                .instanceProps(InstanceProps.builder()
                        .instanceType(InstanceType.of(InstanceClass.BURSTABLE2, InstanceSize.SMALL))
                        .securityGroups(Arrays.asList(rdsSecurityGroup))
                        .allowMajorVersionUpgrade(true)
                        .vpc(this.vpc)
                        .vpcSubnets(SubnetSelection.builder()
                                .subnetType(SubnetType.PRIVATE_WITH_NAT)
                                .build())
                        .build())
                .build());

        return rdsCluster;
    }

    private Role getRDSProxyRole() {
        Role proxyRole = new Role(this.scope, this.logicalNamePrefix + "ProxyRole", RoleProps.builder()
                .roleName(this.physicalNamePrefix + "-rdsproxy-role")
                .assumedBy(new ServicePrincipal("rds.amazonaws.com"))
                .build());
        return proxyRole;
    }

    private DatabaseProxy getRDSProxy(DatabaseCluster rdsCluster, SecurityGroup rdsSecurityGroup, Secret rdsSecret, Role proxyRole) {
        DatabaseProxy proxy = new DatabaseProxy(this.scope, this.logicalNamePrefix + "RDSProxy", DatabaseProxyProps.builder()
                .proxyTarget(ProxyTarget.fromCluster(rdsCluster))
                .securityGroups(Arrays.asList(rdsSecurityGroup))
                .secrets(Arrays.asList(rdsSecret))
                .role(proxyRole)
                .iamAuth(true)
                .requireTls(true)
                .vpc(this.vpc)
                .build());
        return proxy;
    }

    private SecurityGroup getLambdaSecurityGroup() {
        SecurityGroup lambdaSecurityGroup = new SecurityGroup(this.scope, this.logicalNamePrefix + "LambdaSG", SecurityGroupProps.builder()
                .allowAllOutbound(true)
                .securityGroupName(this.physicalNamePrefix + "-lambda-sg")
                .vpc(this.vpc)
                .build());
        return lambdaSecurityGroup;
    }

    private Role getLambdaRole(String account, String region, String dbUserName) {
        Role lambdaRole = new Role(this.scope, this.logicalNamePrefix + "LambdaRole", RoleProps.builder()
                .assumedBy(new ServicePrincipal("lambda.amazonaws.com"))
                .build());
        lambdaRole.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName("service-role/AWSLambdaBasicExecutionRole"));
        lambdaRole.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName("service-role/AWSLambdaVPCAccessExecutionRole"));
        lambdaRole.addToPolicy(new PolicyStatement(PolicyStatementProps.builder()
                .effect(Effect.ALLOW)
                .actions(Arrays.asList("rds-db:connect"))
                .resources(Arrays.asList("arn:aws:rds-db:"
                                + region
                                + ":"
                                + account
                                + ":dbuser:*"
                                + "/" + dbUserName
                        )
                )
                .build()));
        return lambdaRole;
    }

    public void setupDBandProxy() {
        Secret rdsSecret = getRDSSecret();
        SecurityGroup rdsSecurityGroup = getRDSSecurityGroup();
        DatabaseCluster rdsCluster = getRDSCluster(rdsSecret, rdsSecurityGroup);
        Role proxyRole = getRDSProxyRole();
        this.databaseProxy = getRDSProxy(rdsCluster, rdsSecurityGroup, rdsSecret, proxyRole);
        this.lambdaSG = getLambdaSecurityGroup();
        //Self referencing group for RDS Proxy
        rdsSecurityGroup.addIngressRule(rdsSecurityGroup, Port.tcp(Integer.parseInt(dbPort)));
        //Access from lambda to RDS Proxy
        rdsSecurityGroup.addIngressRule(this.lambdaSG, Port.tcp(Integer.parseInt(dbPort)));
        this.lambdaRole = getLambdaRole(account, region, dbUserName);
    }


}
