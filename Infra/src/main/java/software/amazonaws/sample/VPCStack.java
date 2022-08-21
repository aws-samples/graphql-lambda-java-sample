//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package software.amazonaws.sample;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ec2.VpcProps;
import software.constructs.Construct;

public class VPCStack extends Stack {

    private Vpc vpc;

    public VPCStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public VPCStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        this.vpc = new Vpc(this, "TheVPC", VpcProps.builder()
                .maxAzs(2)
                .build());
    }

    public Vpc getVpc() {
        return this.vpc;
    }
}
