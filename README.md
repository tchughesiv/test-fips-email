# Scope
Goal of this project is to create a simple testing environment to replicate the connection issue raised
by Keycloak 22 running in OCP FIPS clusters in `fips` mode when sending emails to the AWS FIPS email endpoint.

Snippet of Keycloak log:
```bash
2023-08-23 07:34:19,358 INFO  [org.bouncycastle.jsse.provider.ProvTlsClient] (executor-thread-5) Client received fatal(2) handshake_failure(40) alert
2023-08-23 07:34:19,481 INFO  [org.bouncycastle.jsse.provider.ProvTlsClient] (executor-thread-5) Client received fatal(2) handshake_failure(40) alert
2023-08-23 07:34:19,481 ERROR [org.keycloak.services] (executor-thread-5) KC-SERVICES0029: Failed to send email: jakarta.mail.MessagingException: Could not connect to SMTP host: email-smtp-fips.us-gov-west-1.amazonaws.com, port: 465;
  nested exception is:
	org.bouncycastle.tls.TlsFatalAlertReceived: handshake_failure(40)
	at org.eclipse.angus.mail.smtp.SMTPTransport.openServer(SMTPTransport.java:2260)
	at org.eclipse.angus.mail.smtp.SMTPTransport.protocolConnect(SMTPTransport.java:753)
	at jakarta.mail.Service.connect(Service.java:342)
	at jakarta.mail.Service.connect(Service.java:222)
	at jakarta.mail.Service.connect(Service.java:243)
	at org.keycloak.email.DefaultEmailSenderProvider.send(DefaultEmailSenderProvider.java:153)
	at org.keycloak.email.DefaultEmailSenderProvider.send(DefaultEmailSenderProvider.java:66)
	at org.keycloak.email.freemarker.FreeMarkerEmailTemplateProvider.send(FreeMarkerEmailTemplateProvider.java:277)
	at org.keycloak.email.freemarker.FreeMarkerEmailTemplateProvider.send(FreeMarkerEmailTemplateProvider.java:271)
	at org.keycloak.email.freemarker.FreeMarkerEmailTemplateProvider.sendSmtpTestEmail(FreeMarkerEmailTemplateProvider.java:128)
```

## Preliminary steps
Install the required TLS resources in `tls` folder:
* cacerts.bcfks: can be copied from the `git@gitlab.cee.redhat.com:service/keycloak.git` repo at `fips-libs/cacerts.bcfks`
* tls.crt: can be extracted from an existing Keycloak namespance NS as `oc extract secret/certificates -n NS --keys tls.crt --confirm --to tls`
* tls.key: can be extracted from an existing Keycloak namespance NS as `oc extract secret/certificates -n NS --keys tls.key --confirm --to tls`

## Testing options
All options are based on a custom image `quay.io/dmartino/ubi9-jdk17-git` including OpenJDK17, git and vim.

## Option 1 - Copy source code on the Pod
Start a Pod with development environment, copy the necessary resources and test it.

```bash
oc apply -f ocp/Deployment.yaml
oc wait --for=condition=Ready pod -l app=test-fips-email
export PODNAME=$(oc get pods -l app=test-fips-email --no-headers -o custom-columns=:metadata.name)

zip -r all.zip src run.sh tls pom.xml
oc cp all.zip ${PODNAME}:/home/default/all.zip
oc rsh ${PODNAME}
```

From the Pod's console, install and launch the application using your specific settings:
* SMTP_USER: the SMTP user (AWS Access Key ID ) 
* SMTP_PWD: the SMTP user password (AWS Secret Access Key)
* TRUSTSTORE_PWD: the password of the `tls/cacerts.bcfks`
* EMAIL: the email recipient and sender 
  
```bash
cd
jar xvf all.zip
chmod +x run.sh
./run.sh SMTP_USER SMTP_PWD TRUSTSTORE_PWD EMAIL
```

## Option 2 - Clone the repo and mount the TLS resources
Start a Pod with the development environment, clone the source from git and link the TLS resources in a Secret.

```bash
oc create secret generic fips-email-tls --from-file tls.crt=tls/tls.crt \
	--from-file tls.key=tls/tls.key \
	--from-file cacerts.bcfks=tls/cacerts.bcfks \
	--from-literal TRUSTSTORE_PWD=YOUR_TRUSTSTORE_PWD
oc apply -f ocp/Deployment-mounted-tls.yaml
oc wait --for=condition=Ready pod -l app=test-fips-email
export PODNAME=$(oc get pods -l app=test-fips-email --no-headers -o custom-columns=:metadata.name)

oc rsh ${PODNAME}
```

Clone the git repo in the pod, link to the mounted TLS resources and run it:

``
git clone https://github.com/dmartinol/test-fips-email.git
cd test-fips-email
rm -rf tls
ln -s /home/default/tls .
./run.sh SMTP_USER SMTP_PWD TRUSTSTORE_PWD EMAIL
```

To rebuild the image run this command and update the deployment configurations in `ocp` folder.
```bash
docker build -t quay.io/YOUR_USER/ubi9-jdk17-git:latest docker
```