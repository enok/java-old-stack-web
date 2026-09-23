# Runtime image for the CampusConnect WAR.
#
# Tomcat 8 on JRE 8 is the deployment target the original team standardised on.
# It is EOL. Replacing it is backlog item B-06, and it cannot happen before the
# javax -> jakarta migration, because Tomcat 10+ will not run javax servlets.
FROM tomcat:8-jre8

LABEL org.campusconnect.synthetic="true"
LABEL org.campusconnect.purpose="before-state of a modernization exercise"

# The stock ROOT app gets in the way of deploying at the context root.
RUN rm -rf /usr/local/tomcat/webapps/ROOT

# TODO CC-1350: the customer code is baked into web.xml at build time, so this
# image is per-customer. One image per institution, three pipelines, three tags.
ARG CUSTOMER_CODE=SUMMIT
ENV CUSTOMER_CODE=${CUSTOMER_CODE}

COPY campusconnect-web/target/campusconnect.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080
CMD ["catalina.sh", "run"]
