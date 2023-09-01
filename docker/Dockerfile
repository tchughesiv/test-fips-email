FROM registry.access.redhat.com/ubi9/openjdk-17:1.15-1.1686736679

USER root
RUN microdnf install -y git vim-enhanced && \
    microdnf clean all

RUN git --version

CMD [ "bash" ]