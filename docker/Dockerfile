FROM registry.access.redhat.com/ubi9/openjdk-17:1.15-1.1686736679 as builder

#USER root
#RUN microdnf install -y git vim-enhanced && \
#    microdnf clean all


#RUN git --version
RUN mkdir test-fips
WORKDIR /home/default/test-fips
RUN pwd
COPY tls tls
COPY src src
COPY pom.xml .
COPY run.sh .
RUN mvn clean install

#RUN cat /usr/share/crypto-policies/FIPS/java.txt
#USER 0
#RUN sed -i 's/TLS_RSA_WITH_AES_128_CBC_SHA, //g' /usr/share/crypto-policies/FIPS/java.txt
#RUN sed -i 's/TLS_RSA_WITH_AES_256_CBC_SHA, //g' /usr/share/crypto-policies/FIPS/java.txt
#RUN echo "cipher@TLS = AES-128-CBC+" > /usr/share/crypto-policies/policies/modules/AWS-FIPS.pmod
#RUN echo "key_exchange = RSA+" >> /usr/share/crypto-policies/policies/modules/AWS-FIPS.pmod
#RUN update-crypto-policies --set FIPS:AWS-FIPS
#USER 185
#RUN cat /usr/share/crypto-policies/FIPS/java.txt
#RUN cat /etc/crypto-policies/back-ends/java.config

FROM quay.io/tchughesiv/kc-dev-fix

COPY --from=builder /home/default/test-fips /app/test-fips

USER 0
RUN echo "cipher = AES-128-CBC+ AES-256-CBC+" > /etc/crypto-policies/policies/modules/AWS-FIPS.pmod
RUN echo "cipher@TLS = AES-128-CBC+ AES-256-CBC+" >> /etc/crypto-policies/policies/modules/AWS-FIPS.pmod
RUN echo "key_exchange = RSA+" >> /etc/crypto-policies/policies/modules/AWS-FIPS.pmod
RUN update-crypto-policies --set FIPS:AWS-FIPS
USER 1000
RUN cat /etc/crypto-policies/state/CURRENT.pol
RUN ls -la /etc/crypto-policies/

WORKDIR /app/test-fips

ENTRYPOINT [ "" ]
