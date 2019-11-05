ARG BUILD_NUMBER
ARG BUILD_URL
ARG CIRCLE_JOB
ARG CIRCLE_WORKFLOW_ID
ARG CIRCLE_WORKFLOW_JOB_ID
ARG CIRCLE_WORKFLOW_UPSTREAM_JOB_IDS
ARG CIRCLE_WORKFLOW_WORKSPACE_ID
ARG DESCRIPTION="Reaction Pricewatch watches prices for change notifications."
ARG DOC_URL=https://github.com/reactioncommerce/pricewatch
ARG NAME=pricewatch
ARG URL=https://github.com/reactioncommerce/pricewatch
ARG VCS_REF
ARG VCS_URL=https://github.com/reactioncommerce/pricewatch
ARG VENDOR="Reaction Commerce, Inc."
ARG VERSION=$VCS_REF


################################################################################
# Development Image
# The development image is intended for running the application dynamically and
# and to provide the best experience for developers. It contains all the
# development and test tooling. It should not be used for production.
################################################################################
FROM clojure:tools-deps-slim-buster AS development

#RUN apk add --update --no-cache \
#      ca-certificates \
#      git \
#      graphviz \
#      libc6-compat

LABEL maintainer="Reaction Commerce <engineering@reactioncommerce.com>" \
      com.reactioncommerce.description=$DESCRIPTION \
      com.reactioncommerce.docker.build.circle.job=$CIRCLE_JOB \
      com.reactioncommerce.docker.build.circle.workflow.id=$CIRCLE_WORKFLOW_ID \
      com.reactioncommerce.docker.build.circle.workflow.job.id=$CIRCLE_WORKFLOW_JOB_ID \
      com.reactioncommerce.docker.build.circle.workflow.upstream.job.ids=$CIRCLE_WORKFLOW_UPSTREAM_JOB_IDS \
      com.reactioncommerce.docker.build.circle.workflow.url=https://circleci.com/workflow-run/$CIRCLE_WORKFLOW_ID \
      com.reactioncommerce.docker.build.circle.workflow.workspace.id=$CIRCLE_WORKFLOW_WORKSPACE_ID \
      com.reactioncommerce.docker.build.number=$BUILD_NUMBER \
      com.reactioncommerce.docker.build.url=$BUILD_URL \
      com.reactioncommerce.license="Copyright © 2018-2019 Reaction Commerce, Inc." \
      com.reactioncommerce.name=$NAME \
      com.reactioncommerce.url=$URL \
      com.reactioncommerce.vcs-ref=$VCS_REF \
      com.reactioncommerce.vcs-url=$VCS_URL \
      com.reactioncommerce.vendor=$VENDOR \
      com.reactioncommerce.version=$VERSION

WORKDIR /usr/src/app

CMD ["bin/run"]



################################################################################
# Build Image
# The build image builds JAR artifacts.
################################################################################
FROM clojure:tools-deps-1.10.0.442-alpine AS build

RUN apk add --update --no-cache \
      ca-certificates \
      git \
      graphviz \
      libc6-compat

COPY . .

# Development Build
RUN ./bin/build-uberjar \
      "/usr/src/app/target/pricewatch.jar" \
      bench:dev:outdated:pack:rebel:test

# Production Build
RUN ./bin/build-uberjar "/usr/src/app/target/pricewatch.jar"



################################################################################
# Production Image
# The production image designed to be as small as possible. It is an Alpine
# image with the JRE runtime and the production JAR file. It doesn't include
# original source code or dev/test tooling.
################################################################################
#FROM openjdk:8-alpine AS production
#FROM adoptopenjdk/openjdk8:alpine-slim AS production
FROM clojure:tools-deps-1.10.0.442-alpine AS production

RUN apk add --update --no-cache \
      ca-certificates \
      libc6-compat

LABEL maintainer="Reaction Commerce <engineering@reactioncommerce.com>" \
      com.reactioncommerce.description=$DESCRIPTION \
      com.reactioncommerce.docker.build.circle.job=$CIRCLE_JOB \
      com.reactioncommerce.docker.build.circle.workflow.id=$CIRCLE_WORKFLOW_ID \
      com.reactioncommerce.docker.build.circle.workflow.job.id=$CIRCLE_WORKFLOW_JOB_ID \
      com.reactioncommerce.docker.build.circle.workflow.upstream.job.ids=$CIRCLE_WORKFLOW_UPSTREAM_JOB_IDS \
      com.reactioncommerce.docker.build.circle.workflow.url=https://circleci.com/workflow-run/$CIRCLE_WORKFLOW_ID \
      com.reactioncommerce.docker.build.circle.workflow.workspace.id=$CIRCLE_WORKFLOW_WORKSPACE_ID \
      com.reactioncommerce.docker.build.number=$BUILD_NUMBER \
      com.reactioncommerce.docker.build.url=$BUILD_URL \
      com.reactioncommerce.license="Copyright © 2018-2019 Reaction Commerce, Inc." \
      com.reactioncommerce.name=$NAME \
      com.reactioncommerce.url=$URL \
      com.reactioncommerce.vcs-ref=$VCS_REF \
      com.reactioncommerce.vcs-url=$VCS_URL \
      com.reactioncommerce.vendor=$VENDOR \
      com.reactioncommerce.version=$VERSION

WORKDIR /usr/src/app

COPY --from=build \
       "/usr/src/app/target/pricewatch.jar" \
       "/usr/src/app/target/pricewatch.jar"
