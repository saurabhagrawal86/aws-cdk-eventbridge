package com.myorg;

import software.amazon.awscdk.core.App;

public class AwsCdkApp {
    public static void main(final String[] args) {
        App app = new App();

        new CdkEventBridgeStack(app, "CdkEventBridgeStack");

        app.synth();
    }
}
