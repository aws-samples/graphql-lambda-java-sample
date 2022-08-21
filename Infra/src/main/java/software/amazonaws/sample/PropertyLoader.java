//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package software.amazonaws.sample;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@NoArgsConstructor
@Getter
@Setter
public class PropertyLoader {

    public Properties loadProperties(String propertiesFilename) {
        Properties prop = new Properties();

        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(propertiesFilename)) {
            if (stream == null) {
                throw new FileNotFoundException();
            }
            prop.load(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return prop;
    }
}
