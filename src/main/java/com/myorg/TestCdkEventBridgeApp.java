package com.myorg;

import software.amazon.awscdk.core.App;

public class TestCdkEventBridgeApp {
    public static void main(final String[] args) {
        App app = new App();

        new TestCdkEventBridgeStack(app, "TestCdkEventBridgeStack");

        app.synth();
    }
}
