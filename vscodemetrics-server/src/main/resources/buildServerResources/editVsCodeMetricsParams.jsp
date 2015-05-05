<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="admin" tagdir="/WEB-INF/tags/admin" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>

<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<jsp:useBean id="constants" class="com.nicologies.vscodemetrics.SettingsBean"/>

<l:settingsGroup title="Visual Studio Code Metrics Power Tool Installation">

  <c:if test="${'auto' == propertiesBean.properties[constants.detectionModeKey]}">
    <c:set var="hidePathInput" value="style='display: none'"/>
  </c:if>

  <c:if test="${'manual' == propertiesBean.properties[constants.detectionModeKey]}">
    <c:set var="hideVersionInput" value="style='display: none'"/>
  </c:if>

  <tr>
    <th><label>Code Metrics detection mode:</label></th>
    <td>
      <c:set var="onclick">
        BS.Util.hide('pathInputSection');
        BS.Util.show('versionInputSection');
      </c:set>
      <props:radioButtonProperty name="${constants.detectionModeKey}"
                                 id="detectionModeAuto"
                                 value="${constants.detectionModeAuto}"
                                 checked="${constants.detectionModeAuto == propertiesBean.properties[constants.detectionModeKey]}"
                                 onclick="${onclick}" />
      <label for="detectionModeAuto">Autodetect installation</label>

      <span style="padding-left: 5em">
        <c:set var="onclick">
          BS.Util.show('pathInputSection');
          BS.Util.hide('versionInputSection');
          BS.VisibilityHandlers.updateVisibility('${constants.rootKey}');
        </c:set>
        <props:radioButtonProperty name="${constants.detectionModeKey}"
                                   id="detectionModeManual"
                                   value="${constants.detectionModeManual}"
                                   checked="${constants.detectionModeManual == propertiesBean.properties[constants.detectionModeKey]}"
                                   onclick="${onclick}"/>
        <label for="detectionModeManual">Specify installation root</label>
      </span>
    </td>
  </tr>

  <tr id="pathInputSection" ${hidePathInput}>
      <th><label for="${constants.rootKey}">Installation root: <l:star/></label></th>
      <td><props:textProperty name="${constants.rootKey}" className="longField"/>
        <span class="error" id="error_${constants.rootKey}"></span>
        <span class="smallNote">The path to the Metrics.exe home directory</span>
      </td>
    </tr>

  <tr id="versionInputSection" ${hideVersionInput}>
    <th><label for="${constants.versionKey}">version: <bs:help file="FxCop" anchor="FxCopVersion"/></label></th>
    <td>
      <props:selectProperty name="${constants.versionKey}">
        <c:forEach var="item" items="${constants.availableVersions}">
          <props:option value="${item.technicalVersionPrefix}"><c:out value="${item.displayName}"/></props:option>
        </c:forEach>
      </props:selectProperty>
      <span class="error" id="error_${constants.versionKey}"></span>
      <span class="smallNote">The FxCop version required by the build; the agent requirement will be created. <br/>
                              To use any version auto-detected on the agent side, select 'Any Detected'.</span>
    </td>
  </tr>

</l:settingsGroup>

<l:settingsGroup title="What To Inspect">
  <tr>
    <c:set var="onclick">
      $('${constants.filesExcludeKey}').disabled = !this.checked;
      $('${constants.filesKey}').focus();
      BS.VisibilityHandlers.updateVisibility('${constants.filesKey}');
    </c:set>

    <th>
      <label for="mod-files">Assemblies: </label></th>
    <td><span>
      <props:multilineProperty name="${constants.filesKey}"
                               className="longField"
                               linkTitle="Type assembly files or wildcards"
                               cols="55" rows="5"
                               expanded="true"
                               note="${note}"/>
      <c:set var="note">Assembly file names relative to the checkout root separated by spaces.<br/>
        Ant-like wildcards are supported.<br/>
        Example: bin*.dll</c:set>
      <props:multilineProperty name="${constants.filesExcludeKey}"
                               className="longField"
                               linkTitle="Exclude assembly files by wildcard"
                               cols="55" rows="5"
                               expanded="false"
                               note="${note}"/>
      </span>
    </td>
  </tr>
</l:settingsGroup>

<l:settingsGroup title="Metrics Options" className="advancedSetting">
  <tr class="advancedSetting">
    <th><label for="${constants.searchInGacKey}">Search referenced assemblies in GAC</label></th>
    <td>
      <props:checkboxProperty name="${constants.searchInGacKey}"/>
      <span class="error" id="error_${constants.searchInGacKey}"></span>
    </td>
  </tr>
  <tr class="advancedSetting">
    <th><label for="${constants.searchDirsKey}">Search referenced assemblies in directories</label></th>
    <td>
      <props:textProperty name="${constants.searchDirsKey}" className="longField"/>
      <span class="error" id="error_${constants.searchDirsKey}"></span>
      <span class="smallNote">The space-separated list of directories relative to the checkout root. <br/>
                              Sets /d: options for Code Metrics</span>
    </td>
  </tr>
  <tr class="advancedSetting">
    <th><label for="${constants.ignoreGeneratedCodeKey}">Ignore generated code</label></th>
    <td>
      <props:checkboxProperty name="${constants.ignoreGeneratedCodeKey}"/>
      <span class="error" id="error_${constants.ignoreGeneratedCodeKey}"></span>
      <span class="smallNote">Sets /ignoregeneratedcode</span>
    </td>
  </tr>
  <tr class="advancedSetting">
    <th><label for="${constants.addtionalOptionsKey}">Additional Cmd options: </label></th>
    <td><props:textProperty name="${constants.addtionalOptionsKey}" className="longField"/>
      <span class="error" id="error_${constants.addtionalOptionsKey}"></span>
      <span class="smallNote">Additional options for Metrics.exe command line</span>
    </td>
  </tr>
</l:settingsGroup>

<l:settingsGroup title="Build Failure Conditions">

  <tr class="advancedSetting">
    <th><label for="${constants.failOnAnalysisErrorKey}">Fail on analysis errors</label></th>
    <td>
      <props:checkboxProperty name="${constants.failOnAnalysisErrorKey}"/>
      <span class="error" id="error_${constants.failOnAnalysisErrorKey}"></span>
      <span class="smallNote">Fails build on analysis errors such as:<br/>
        ANALYSIS_ERROR ASSEMBLY_LOAD_ERROR, ASSEMBLY_REFERENCES_ERROR, PROJECT_LOAD_ERROR, RULE_LIBRARY_LOAD_ERROR, UNKNOWN_ERROR, OUTPUT_ERROR
      </span>
    </td>
  </tr>

  <tr>
    <th colspan="2">You can configure a build to fail if it has too many inspection errors or warnings. To do so, add a corresponding
      <c:set var="editFailureCondLink"><admin:editBuildTypeLink step="buildFailureConditions" buildTypeId="${buildForm.settings.externalId}" withoutLink="true"/></c:set>
      <a href="${editFailureCondLink}#addFeature=BuildFailureOnMetric">build failure condition</a>.
    </th>
  </tr>

</l:settingsGroup>
