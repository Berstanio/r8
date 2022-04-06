// Copyright (c) 2022, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.android.tools.r8.experimental.startup;

import static com.android.tools.r8.utils.InternalOptions.isSystemPropertyForDevelopmentSet;

public class StartupOptions {

  private boolean enableMinimalStartupDex =
      isSystemPropertyForDevelopmentSet("com.android.tools.r8.startup.minimalstartupdex");
  private boolean enableStartupCompletenessCheckForTesting =
      isSystemPropertyForDevelopmentSet("com.android.tools.r8.startup.completenesscheck");

  private StartupConfiguration startupConfiguration;

  public boolean isMinimalStartupDexEnabled() {
    return enableMinimalStartupDex;
  }

  public StartupOptions setEnableMinimalStartupDex() {
    enableMinimalStartupDex = true;
    return this;
  }

  public boolean isStartupCompletenessCheckForTesting() {
    return enableStartupCompletenessCheckForTesting;
  }

  public StartupOptions setEnableStartupCompletenessCheckForTesting() {
    enableStartupCompletenessCheckForTesting = true;
    return this;
  }

  public boolean hasStartupConfiguration() {
    return startupConfiguration != null;
  }

  public StartupConfiguration getStartupConfiguration() {
    return startupConfiguration;
  }

  public void setStartupConfiguration(StartupConfiguration startupConfiguration) {
    this.startupConfiguration = startupConfiguration;
  }
}
