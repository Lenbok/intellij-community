/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

/*
 * User: anna
 * Date: 14-May-2007
 */
package com.intellij.execution.configuration;

import com.intellij.execution.ExecutionBundle;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Comparing;
import com.intellij.ui.UserActivityProviderComponent;
import com.intellij.util.ArrayUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.ChangeListener;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class EnvironmentVariablesComponent extends LabeledComponent<TextFieldWithBrowseButton> implements UserActivityProviderComponent {
  @NonNls private static final String ENVS = "envs";
  @NonNls public static final String ENV = "env";
  @NonNls public static final String NAME = "name";
  @NonNls public static final String VALUE = "value";
  @NonNls private static final String OPTION = "option";
  @NonNls private static final String ENV_VARIABLES = "ENV_VARIABLES";

  private final EnvironmentVariablesTextField myEnvsTextField;

  public EnvironmentVariablesComponent() {
    super();
    myEnvsTextField = new EnvironmentVariablesTextField();
    setComponent(myEnvsTextField.getComponent());
    setText(ExecutionBundle.message("environment.variables.component.title"));
  }

  public void setEnvs(@NotNull Map<String, String> envs) {
    myEnvsTextField.setEnvs(envs);
  }

  @NotNull
  public Map<String, String> getEnvs() {
    return myEnvsTextField.getEnvs();
  }

  public boolean isPassParentEnvs() {
    return myEnvsTextField.isPassParentEnvs();
  }

  public void setPassParentEnvs(final boolean passParentEnvs) {
    myEnvsTextField.setPassParentEnvs(passParentEnvs);
  }

  public static void readExternal(Element element, Map<String, String> envs) {
    final Element envsElement = element.getChild(ENVS);
    if (envsElement != null) {
      for (Object o : envsElement.getChildren(ENV)) {
        Element envElement = (Element)o;
        final String envName = envElement.getAttributeValue(NAME);
        final String envValue = envElement.getAttributeValue(VALUE);
        if (envName != null && envValue != null) {
          envs.put(envName, envValue);
        }
      }
    } else { //compatibility with prev version
      for (Object o : element.getChildren(OPTION)) {
        if (Comparing.strEqual(((Element)o).getAttributeValue(NAME), ENV_VARIABLES)) {
          splitVars(envs, ((Element)o).getAttributeValue(VALUE));
          break;
        }
      }
    }
  }

  private static void splitVars(final Map<String, String> envs, final String val) {
    if (val != null) {
      final String[] envVars = val.split(";");
      if (envVars != null) {
        for (String envVar : envVars) {
          final int idx = envVar.indexOf('=');
          if (idx > -1) {
            envs.put(envVar.substring(0, idx), idx < envVar.length() - 1 ? envVar.substring(idx + 1) : "");
          }
        }
      }
    }
  }

  public static void writeExternal(Element element, Map<String, String> envs) {
    final Element envsElement = new Element(ENVS);
    for (String envName : envs.keySet()) {
      final Element envElement = new Element(ENV);
      envElement.setAttribute(NAME, envName);
      envElement.setAttribute(VALUE, envs.get(envName));
      envsElement.addContent(envElement);
    }
    element.addContent(envsElement);
  }

  public static void inlineParentOccurrences(final Map<String, String> envs) {
    final Map<String, String> parentParams = new HashMap<String, String>(System.getenv());
    for (String envKey : envs.keySet()) {
      final String val = envs.get(envKey);
      if (val != null) {
        final String parentVal = parentParams.get(envKey);
        if (parentVal != null && containsEnvKeySubstitution(envKey, val)) {
          envs.put(envKey, val.replace("$" + envKey + "$", parentVal));
        }
      }
    }
  }

  public static boolean containsEnvKeySubstitution(final String envKey, final String val) {
    return ArrayUtil.find(val.split(File.pathSeparator), "$" + envKey + "$") != -1;
  }

  @Override
  public void addChangeListener(final ChangeListener changeListener) {
    myEnvsTextField.addChangeListener(changeListener);
  }

  @Override
  public void removeChangeListener(final ChangeListener changeListener) {
    myEnvsTextField.removeChangeListener(changeListener);
  }
}
