#!/bin/bash
#
# serenity.sh
# create the file structure for a new serenity tool with service

if [ $# -ne 1 ]
then
echo "usage: serenity APPNAME    like: serenity Coursemap"
exit -1
fi

# add to master/pom.xml
# add to root pom.xml
# mvn clean install in master
# mvn eclipse:eclipse in root
# import into eclipse

# as given, the class name with camel case
APPNAMECC=$1
# lower case for paths and file names
APPNAME=$(echo $APPNAMECC | tr '[:upper:]' '[:lower:]')

echo creating $APPNAME for class $APPNAMECC

cd ~/dev/serenity

# export from svn the template app
svn --force export https://source.etudes.org/svn/serenity/trunk/template ${APPNAME} > /dev/null

# renames
mv ${APPNAME}/template-api ${APPNAME}/${APPNAME}-api
mv ${APPNAME}/${APPNAME}-api/src/main/java/org/etudes/template ${APPNAME}/${APPNAME}-api/src/main/java/org/etudes/${APPNAME}
mv ${APPNAME}/${APPNAME}-api/src/main/java/org/etudes/${APPNAME}/api/TemplateService.java ${APPNAME}/${APPNAME}-api/src/main/java/org/etudes/${APPNAME}/api/${APPNAMECC}Service.java

mv ${APPNAME}/template-webapp ${APPNAME}/${APPNAME}-webapp
mv ${APPNAME}/${APPNAME}-webapp/src/main/resources/sql/template.sql ${APPNAME}/${APPNAME}-webapp/src/main/resources/sql/${APPNAME}.sql
mv ${APPNAME}/${APPNAME}-webapp/src/main/webapp/template.js ${APPNAME}/${APPNAME}-webapp/src/main/webapp/${APPNAME}.js
mv ${APPNAME}/${APPNAME}-webapp/src/main/webapp/template.html ${APPNAME}/${APPNAME}-webapp/src/main/webapp/${APPNAME}.html
mv ${APPNAME}/${APPNAME}-webapp/src/main/webapp/template_i10n.js ${APPNAME}/${APPNAME}-webapp/src/main/webapp/${APPNAME}_i10n.js
mv ${APPNAME}/${APPNAME}-webapp/src/main/java/org/etudes/template ${APPNAME}/${APPNAME}-webapp/src/main/java/org/etudes/${APPNAME}
mv ${APPNAME}/${APPNAME}-webapp/src/main/java/org/etudes/${APPNAME}/webapp/TemplateServiceImpl.java ${APPNAME}/${APPNAME}-webapp/src/main/java/org/etudes/${APPNAME}/webapp/${APPNAMECC}ServiceImpl.java
mv ${APPNAME}/${APPNAME}-webapp/src/main/java/org/etudes/${APPNAME}/webapp/TemplateCdpHandler.java ${APPNAME}/${APPNAME}-webapp/src/main/java/org/etudes/${APPNAME}/webapp/${APPNAMECC}CdpHandler.java
mv ${APPNAME}/${APPNAME}-webapp/src/main/java/org/etudes/${APPNAME}/webapp/TemplateServlet.java ${APPNAME}/${APPNAME}-webapp/src/main/java/org/etudes/${APPNAME}/webapp/${APPNAMECC}Servlet.java

# text replace all "template" with ${APPNAME}, "Template" with ${APPNAMECC}
perl -pi -w -e s/template/${APPNAME}/g ${APPNAME}/pom.xml

perl -pi -w -e s/template/${APPNAME}/g ${APPNAME}/${APPNAME}-api/pom.xml
perl -pi -w -e s/template/${APPNAME}/g ${APPNAME}/${APPNAME}-api/src/main/java/org/etudes/${APPNAME}/api/${APPNAMECC}Service.java
perl -pi -w -e s/Template/${APPNAMECC}/g ${APPNAME}/${APPNAME}-api/src/main/java/org/etudes/${APPNAME}/api/${APPNAMECC}Service.java

perl -pi -w -e s/template/${APPNAME}/g ${APPNAME}/${APPNAME}-webapp/pom.xml
perl -pi -w -e s/template/${APPNAME}/g ${APPNAME}/${APPNAME}-webapp/src/main/webapp/WEB-INF/web.xml
perl -pi -w -e s/Template/${APPNAMECC}/g ${APPNAME}/${APPNAME}-webapp/src/main/webapp/WEB-INF/web.xml

perl -pi -w -e s/template/${APPNAME}/g ${APPNAME}/${APPNAME}-webapp/src/main/webapp/${APPNAME}.js
perl -pi -w -e s/Template/${APPNAMECC}/g ${APPNAME}/${APPNAME}-webapp/src/main/webapp/${APPNAME}.js
perl -pi -w -e s/template/${APPNAME}/g ${APPNAME}/${APPNAME}-webapp/src/main/webapp/${APPNAME}_i10n.js
perl -pi -w -e s/Template/${APPNAMECC}/g ${APPNAME}/${APPNAME}-webapp/src/main/webapp/${APPNAME}_i10n.js
perl -pi -w -e s/template/${APPNAME}/g ${APPNAME}/${APPNAME}-webapp/src/main/webapp/${APPNAME}.html
perl -pi -w -e s/Template/${APPNAMECC}/g ${APPNAME}/${APPNAME}-webapp/src/main/webapp/${APPNAME}.html

perl -pi -w -e s/template/${APPNAME}/g ${APPNAME}/${APPNAME}-webapp/src/main/java/org/etudes/${APPNAME}/webapp/${APPNAMECC}ServiceImpl.java
perl -pi -w -e s/Template/${APPNAMECC}/g ${APPNAME}/${APPNAME}-webapp/src/main/java/org/etudes/${APPNAME}/webapp/${APPNAMECC}ServiceImpl.java
perl -pi -w -e s/template/${APPNAME}/g ${APPNAME}/${APPNAME}-webapp/src/main/java/org/etudes/${APPNAME}/webapp/${APPNAMECC}CdpHandler.java
perl -pi -w -e s/Template/${APPNAMECC}/g ${APPNAME}/${APPNAME}-webapp/src/main/java/org/etudes/${APPNAME}/webapp/${APPNAMECC}CdpHandler.java
perl -pi -w -e s/template/${APPNAME}/g ${APPNAME}/${APPNAME}-webapp/src/main/java/org/etudes/${APPNAME}/webapp/${APPNAMECC}Servlet.java
perl -pi -w -e s/Template/${APPNAMECC}/g ${APPNAME}/${APPNAME}-webapp/src/main/java/org/etudes/${APPNAME}/webapp/${APPNAMECC}Servlet.java

svn add ${APPNAME} > /dev/null
svn propset svn:ignore -F svn_ignore ${APPNAME}/${APPNAME}-api/ > /dev/null
svn propset svn:ignore -F svn_ignore ${APPNAME}/${APPNAME}-webapp/ > /dev/null
