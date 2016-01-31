<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<jsp:useBean id="constants" class="com.nicologies.vscodemetrics.SettingsBean"/>

<c:choose>
  <c:when test="${propertiesBean.properties[constants.detectionModeKey] == constants.detectionModeAuto}">
    <div class="parameter">
      Installation root: <strong>auto detected</strong>
    </div>
    <div class="parameter">
      Version: <strong><props:displayValue name="${constants.versionKey}" emptyValue="any detected"/></strong>
    </div>
  </c:when>
  <c:otherwise>
    <div class="parameter">
      Installation root: <strong><props:displayValue name="${constants.rootKey}" emptyValue="not specified"/></strong>
    </div>
  </c:otherwise>
</c:choose>

<div class="parameter">
  Assemblies to inspect: <strong><props:displayValue name="${constants.filesKey}" emptyValue="not specified"/></strong>
</div>
<div class="parameter">
  Assemblies to exclude: <strong><props:displayValue name="${constants.filesExcludeKey}" emptyValue="not specified"/></strong>
</div>
<div class="parameter">
  Assemblies with company name starts with: <strong><props:displayValue name="${constants.companyNameKey}" emptyValue="not specified"/></strong>
</div>

<div class="parameter">
  Additional Cmd options: <strong><props:displayValue name="${constants.additionalOptionsKey}" emptyValue="not specified"/></strong>
</div>

<div class="parameter">
  Ignore generated code: <strong><props:displayCheckboxValue name="${constants.ignoreGeneratedCodeKey}"/></strong>
</div>

<div class="parameter">
  Search dependencies in GAC: <strong><props:displayCheckboxValue name="${constants.searchInGacKey}"/></strong>
</div>

<div class="parameter">
  Search dependencies in directories: <strong><props:displayValue name="${constants.searchDirsKey}" emptyValue="empty list" /></strong>
</div>

<div class="parameter">
  Fail on analysis errors: <strong><props:displayCheckboxValue name="${constants.failOnAnalysisErrorKey}"/></strong>
</div>
