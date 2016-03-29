<%@ page contentType="text/html;charset=UTF-8" language="java" session="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags"%>

<jsp:useBean id="buildData" type="jetbrains.buildServer.serverSide.SBuild" scope="request"/>
<%--<div><canvas id="myChart"></canvas></div>--%>
<c:if test="${empty notReady}">
<%--
    <div class="attentionComment" style="margin-bottom: 10px">
      Save to compare with other results
      <a class="btn btn_mini" onclick="$('buildResults').refresh(null, 'runningBuildRefresh=1'); return false" href="#">Save</a>
    </div>
--%>
    ${html}
</c:if>

<c:if test="${not empty notReady}">
    ${notReady}
</c:if>
<%--
<c:url value='${teamcityPluginResourcesPath}/Chart.min.js' var='chartJs'/>
    <script src='${chartJs}'></script>

    <c:url value='${teamcityPluginResourcesPath}/mychart.js' var='myChartJs'/>
    <script src='${myChartJs}'></script>
--%>