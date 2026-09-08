-- Runs once, on the first initialisation of an empty Postgres data volume.
-- Creates the engine database shared with workflow-service.
-- (POSTGRES_DB=collector is created automatically by the image; this adds the
--  workflow_service database the Flowable UI and workflow-service both use.)
CREATE DATABASE workflow_service;
