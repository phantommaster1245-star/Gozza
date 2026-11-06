package com.fulllock.app;

public class FullLockApp extends android.app.Application {
    @Override public void onCreate(){super.onCreate();initialize(this);}
    static void initialize(android.app.Application app){Store.prefs(app).edit().putBoolean("floatingEnabled",false).putBoolean("enabled",false).putBoolean("autoAccept",false).putBoolean("pickupDirectSend",true).putBoolean("cardShowPickup",true).apply();AppIssues.install(app);RootMode.retireUnused(app);Profiles.initialize(app);if(Store.recoverInterruptedOpenings(app)&&RootMode.enabled(app))AppIssues.interrupted(app);RootSetup.updatePending=RootMode.enabled(app)&&RootSetup.needsUpgrade(app);if(RootMode.enabled(app)&&!RootSetup.updatePending)LocalRelayClient.start(app);}
}
