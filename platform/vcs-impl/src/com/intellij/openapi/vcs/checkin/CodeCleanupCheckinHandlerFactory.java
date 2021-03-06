/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.openapi.vcs.checkin;

import com.intellij.analysis.AnalysisScope;
import com.intellij.codeInspection.ex.GlobalInspectionContextBase;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.VcsConfiguration;
import com.intellij.openapi.vcs.changes.CommitContext;
import com.intellij.openapi.vcs.ui.RefreshableOnComponent;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;


public class CodeCleanupCheckinHandlerFactory extends CheckinHandlerFactory  {
  @NotNull
  public CheckinHandler createHandler(final CheckinProjectPanel panel, CommitContext commitContext) {
    return new CleanupCodeCheckinHandler(panel);
  }

  private static class CleanupCodeCheckinHandler extends CheckinHandler implements CheckinMetaHandler {
    private final CheckinProjectPanel myPanel;
    private Project myProject;

    public CleanupCodeCheckinHandler(CheckinProjectPanel panel) {
      myProject = panel.getProject();
      myPanel = panel;
    }

    @Override
    public RefreshableOnComponent getBeforeCheckinConfigurationPanel() {
      final JCheckBox cleanupCodeCb = new JCheckBox("Cleanup code");
      return new RefreshableOnComponent() {
        public JComponent getComponent() {
          final JPanel cbPanel = new JPanel(new BorderLayout());
          cbPanel.add(cleanupCodeCb, BorderLayout.WEST);
          CheckinHandlerUtil
            .disableWhenDumb(myProject, cleanupCodeCb, "Code analysis is impossible until indices are up-to-date");
          return cbPanel;
        }

        public void refresh() {
        }

        public void saveState() {
          VcsConfiguration.getInstance(myProject).CHECK_CODE_CLEANUP_BEFORE_PROJECT_COMMIT = cleanupCodeCb.isSelected();
        }

        public void restoreState() {
          cleanupCodeCb.setSelected(VcsConfiguration.getInstance(myProject).CHECK_CODE_CLEANUP_BEFORE_PROJECT_COMMIT);
        }
      };
    }


    @Override
    public void runCheckinHandlers(Runnable runnable) {
      if (VcsConfiguration.getInstance(myProject).CHECK_CODE_CLEANUP_BEFORE_PROJECT_COMMIT  && !DumbService.isDumb(myProject)) {

        GlobalInspectionContextBase.codeCleanup(myProject, new AnalysisScope(myProject, myPanel.getVirtualFiles()), runnable);

      } else {
        runnable.run();
      }
    }
  }
}