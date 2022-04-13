-- noinspection SqlNoDataSourceInspectionForFile

-- noinspection SqlInsertValuesForFile

-- data populated for the integration tests
-- SQL needs to be defined in terms of autogenerated HSQL table structure, not the actual PostgreSQL tables.
INSERT INTO UI_USERS(ID, USERNAME, LOCALE, CREATED_AT, UPDATED_AT) values (1000001, 'testuser', null, now(), now());
-- noinspection SqlResolve
INSERT INTO APIKEY (ID, ENCODED_KEY) values (1, 'ad26a8235b3e847dc0b9ac34733d5acb39e2b6af634796e7eebe171165cdf2d1');
INSERT INTO APIKEY_ROLES (APIKEY_ID, ROLE) values (1, 'XROAD_SYSTEM_ADMINISTRATOR');
INSERT INTO APIKEY_ROLES (APIKEY_ID, ROLE) values (1, 'XROAD_SECURITY_OFFICER');
INSERT INTO APIKEY_ROLES (APIKEY_ID, ROLE) values (1, 'XROAD_REGISTRATION_OFFICER');

INSERT INTO MEMBER_CLASSES (ID, CODE, DESCRIPTION, CREATED_AT, UPDATED_AT) values (1000001, 'GOV', 'Government', now(), now());
INSERT INTO MEMBER_CLASSES (ID, CODE, DESCRIPTION, CREATED_AT, UPDATED_AT) values (1000002, 'MemberclassFoo', 'Member class with rare code string', now(), now());
INSERT INTO MEMBER_CLASSES (ID, CODE, DESCRIPTION, CREATED_AT, UPDATED_AT) VALUES (701, 'ORG', 'Organizations', '2022-03-07 07:33:22.654029', '2022-03-07 07:33:22.654029');

INSERT INTO IDENTIFIERS(ID, OBJECT_TYPE, XROAD_INSTANCE, MEMBER_CLASS, MEMBER_CODE, SUBSYSTEM_CODE, SERVICE_CODE, SERVER_CODE, TYPE, SERVICE_VERSION) VALUES (701, 'MEMBER', 'TEST', 'ORG', '111', null, null, null, 'ClientId', null);
INSERT INTO IDENTIFIERS(ID, OBJECT_TYPE, XROAD_INSTANCE, MEMBER_CLASS, MEMBER_CODE, SUBSYSTEM_CODE, SERVICE_CODE, SERVER_CODE, TYPE, SERVICE_VERSION) VALUES (702, 'SUBSYSTEM', 'TEST', 'ORG', '111', 'MANAGEMENT', null, null, 'ClientId', null);
INSERT INTO IDENTIFIERS(ID, OBJECT_TYPE, XROAD_INSTANCE, MEMBER_CLASS, MEMBER_CODE, SUBSYSTEM_CODE, SERVICE_CODE, SERVER_CODE, TYPE, SERVICE_VERSION) VALUES (703, 'SERVER', 'TEST', 'ORG', '111', null, null, 'ADMINSS', 'SecurityServerId', null);
INSERT INTO IDENTIFIERS(ID, OBJECT_TYPE, XROAD_INSTANCE, MEMBER_CLASS, MEMBER_CODE, SUBSYSTEM_CODE, SERVICE_CODE, SERVER_CODE, TYPE, SERVICE_VERSION) VALUES (704, 'SUBSYSTEM', 'TEST', 'ORG', '222', null, null, 'SERVICESS', 'SecurityServerId', null);
INSERT INTO IDENTIFIERS(ID, OBJECT_TYPE, XROAD_INSTANCE, MEMBER_CLASS, MEMBER_CODE, SUBSYSTEM_CODE, SERVICE_CODE, SERVER_CODE, TYPE, SERVICE_VERSION) VALUES (705, 'MEMBER', 'TEST', 'GOV', '333', null, null, null, 'ClientId', null);
INSERT INTO IDENTIFIERS(ID, OBJECT_TYPE, XROAD_INSTANCE, MEMBER_CLASS, MEMBER_CODE, SUBSYSTEM_CODE, SERVICE_CODE, SERVER_CODE, TYPE, SERVICE_VERSION) VALUES (706, 'MEMBER', 'TEST', 'ORG', '000', null, null, null, 'ClientId', null);
INSERT INTO IDENTIFIERS(ID, OBJECT_TYPE, XROAD_INSTANCE, MEMBER_CLASS, MEMBER_CODE, SUBSYSTEM_CODE, SERVICE_CODE, SERVER_CODE, TYPE, SERVICE_VERSION) VALUES (707, 'MEMBER', 'TEST', 'GOV', '321', null, null, null, 'ClientId', null);
INSERT INTO IDENTIFIERS (ID, OBJECT_TYPE, XROAD_INSTANCE, MEMBER_CLASS, MEMBER_CODE, SUBSYSTEM_CODE, TYPE) values (1000001, 'MEMBER', 'TEST', 'GOV', 'M1', null, 'ClientId');
INSERT INTO IDENTIFIERS (ID, OBJECT_TYPE, XROAD_INSTANCE, MEMBER_CLASS, MEMBER_CODE, SUBSYSTEM_CODE, TYPE) values (1000002, 'MEMBER', 'TEST', 'GOV', 'M2', null, 'ClientId');
INSERT INTO IDENTIFIERS (ID, OBJECT_TYPE, XROAD_INSTANCE, MEMBER_CLASS, MEMBER_CODE, SUBSYSTEM_CODE, TYPE) values (1000003, 'MEMBER', 'TEST', 'GOV', 'M3', null, 'ClientId');
INSERT INTO IDENTIFIERS (ID, OBJECT_TYPE, XROAD_INSTANCE, MEMBER_CLASS, MEMBER_CODE, SUBSYSTEM_CODE, TYPE) values (1000004, 'MEMBER', 'TEST', 'GOV', 'M4', null, 'ClientId');
INSERT INTO IDENTIFIERS (ID, OBJECT_TYPE, XROAD_INSTANCE, MEMBER_CLASS, MEMBER_CODE, SUBSYSTEM_CODE, TYPE) values (1000005, 'MEMBER', 'TEST', 'GOV', 'M5', null, 'ClientId');
INSERT INTO IDENTIFIERS (ID, OBJECT_TYPE, XROAD_INSTANCE, MEMBER_CLASS, MEMBER_CODE, SUBSYSTEM_CODE, TYPE) values (1000006, 'MEMBER', 'TEST', 'GOV', 'M6', null, 'ClientId');
INSERT INTO IDENTIFIERS (ID, OBJECT_TYPE, XROAD_INSTANCE, MEMBER_CLASS, MEMBER_CODE, SUBSYSTEM_CODE, TYPE) values (1000007, 'MEMBER', 'TEST', 'GOV', 'M7', null, 'ClientId');
INSERT INTO IDENTIFIERS (ID, OBJECT_TYPE, XROAD_INSTANCE, MEMBER_CLASS, MEMBER_CODE, SUBSYSTEM_CODE, TYPE) values (1000008, 'MEMBER', 'TEST', 'GOV', 'M8', null, 'ClientId');
INSERT INTO IDENTIFIERS (ID, OBJECT_TYPE, XROAD_INSTANCE, MEMBER_CLASS, MEMBER_CODE, SUBSYSTEM_CODE, TYPE) values (1000009, 'MEMBER', 'TEST', 'GOV', 'M9', null, 'ClientId');
INSERT INTO IDENTIFIERS (ID, OBJECT_TYPE, XROAD_INSTANCE, MEMBER_CLASS, MEMBER_CODE, SUBSYSTEM_CODE, TYPE) values (1000010, 'SUBSYSTEM', 'TEST', 'GOV', 'M1', 'SS1', 'ClientId');
INSERT INTO IDENTIFIERS (ID, OBJECT_TYPE, XROAD_INSTANCE, MEMBER_CLASS, MEMBER_CODE, SUBSYSTEM_CODE, TYPE) values (1000011, 'SUBSYSTEM', 'TEST', 'MemberclassFoo', 'M10', null, 'ClientId');
INSERT INTO IDENTIFIERS (ID, OBJECT_TYPE, XROAD_INSTANCE, MEMBER_CLASS, MEMBER_CODE, SUBSYSTEM_CODE, TYPE) values (1000012, 'MEMBER', 'Instance2', 'GOV', 'M11', null, 'ClientId');


INSERT INTO SECURITY_SERVER_CLIENTS (ID, MEMBER_CODE, SUBSYSTEM_CODE, NAME, XROAD_MEMBER_ID, MEMBER_CLASS_ID, SERVER_CLIENT_ID, TYPE, CREATED_AT, UPDATED_AT) values (1000001, 'M1', null, 'Member1', null, 1000001, 1000001, 'XRoadMember', now(), now());
INSERT INTO SECURITY_SERVER_CLIENTS (ID, MEMBER_CODE, SUBSYSTEM_CODE, NAME, XROAD_MEMBER_ID, MEMBER_CLASS_ID, SERVER_CLIENT_ID, TYPE, CREATED_AT, UPDATED_AT) values (1000002, 'M2', null, 'Member2', null, 1000001, 1000002, 'XRoadMember', now(), now());
INSERT INTO SECURITY_SERVER_CLIENTS (ID, MEMBER_CODE, SUBSYSTEM_CODE, NAME, XROAD_MEMBER_ID, MEMBER_CLASS_ID, SERVER_CLIENT_ID, TYPE, CREATED_AT, UPDATED_AT) values (1000003, 'M3', null, 'member3', null, 1000001, 1000003, 'XRoadMember', now(), now());
INSERT INTO SECURITY_SERVER_CLIENTS (ID, MEMBER_CODE, SUBSYSTEM_CODE, NAME, XROAD_MEMBER_ID, MEMBER_CLASS_ID, SERVER_CLIENT_ID, TYPE, CREATED_AT, UPDATED_AT) values (1000004, 'M4', null, 'mEmber4', null, 1000001, 1000004, 'XRoadMember', now(), now());
INSERT INTO SECURITY_SERVER_CLIENTS (ID, MEMBER_CODE, SUBSYSTEM_CODE, NAME, XROAD_MEMBER_ID, MEMBER_CLASS_ID, SERVER_CLIENT_ID, TYPE, CREATED_AT, UPDATED_AT) values (1000005, 'M5', null, 'Member5-ÅÖÄ', null, 1000001, 1000005, 'XRoadMember', now(), now());
INSERT INTO SECURITY_SERVER_CLIENTS (ID, MEMBER_CODE, SUBSYSTEM_CODE, NAME, XROAD_MEMBER_ID, MEMBER_CLASS_ID, SERVER_CLIENT_ID, TYPE, CREATED_AT, UPDATED_AT) values (1000006, 'M6', null, 'Member6\a', null, 1000001, 1000006, 'XRoadMember', now(), now());
INSERT INTO SECURITY_SERVER_CLIENTS (ID, MEMBER_CODE, SUBSYSTEM_CODE, NAME, XROAD_MEMBER_ID, MEMBER_CLASS_ID, SERVER_CLIENT_ID, TYPE, CREATED_AT, UPDATED_AT) values (1000007, 'M7', null, 'Member7_a', null, 1000001, 1000007, 'XRoadMember', now(), now());
INSERT INTO SECURITY_SERVER_CLIENTS (ID, MEMBER_CODE, SUBSYSTEM_CODE, NAME, XROAD_MEMBER_ID, MEMBER_CLASS_ID, SERVER_CLIENT_ID, TYPE, CREATED_AT, UPDATED_AT) values (1000008, 'M8', null, 'Member8%a', null, 1000001, 1000008, 'XRoadMember', now(), now());
INSERT INTO SECURITY_SERVER_CLIENTS (ID, MEMBER_CODE, SUBSYSTEM_CODE, NAME, XROAD_MEMBER_ID, MEMBER_CLASS_ID, SERVER_CLIENT_ID, TYPE, CREATED_AT, UPDATED_AT) values (1000009, 'M9', null, 'Member9__%%em%', null, 1000001, 1000009, 'XRoadMember', now(), now());
INSERT INTO SECURITY_SERVER_CLIENTS (ID, MEMBER_CODE, SUBSYSTEM_CODE, NAME, XROAD_MEMBER_ID, MEMBER_CLASS_ID, SERVER_CLIENT_ID, TYPE, CREATED_AT, UPDATED_AT) values (1000010, null, 'SS1', 'Member1-SS1', 1000001, 1000001, 1000010, 'Subsystem', now(), now());
INSERT INTO SECURITY_SERVER_CLIENTS (ID, MEMBER_CODE, SUBSYSTEM_CODE, NAME, XROAD_MEMBER_ID, MEMBER_CLASS_ID, SERVER_CLIENT_ID, TYPE, CREATED_AT, UPDATED_AT) values (1000011, 'M10', null, 'Member10', null, 1000002, 1000011, 'XRoadMember', now(), now());
INSERT INTO SECURITY_SERVER_CLIENTS (ID, MEMBER_CODE, SUBSYSTEM_CODE, NAME, XROAD_MEMBER_ID, MEMBER_CLASS_ID, SERVER_CLIENT_ID, TYPE, CREATED_AT, UPDATED_AT) values (1000012, 'M11', null, 'Member11', null, 1000001, 1000012, 'XRoadMember', now(), now());
INSERT INTO SECURITY_SERVER_CLIENTS (ID, MEMBER_CODE, SUBSYSTEM_CODE, NAME, XROAD_MEMBER_ID, MEMBER_CLASS_ID, SERVER_CLIENT_ID, TYPE, ADMINISTRATIVE_CONTACT, CREATED_AT, UPDATED_AT) VALUES (701, '111', null, 'ADMORG', null, 701, 701, 'XRoadMember', null, '2022-03-07 07:44:57.466408', '2022-03-07 07:44:57.466408');
INSERT INTO SECURITY_SERVER_CLIENTS (ID, MEMBER_CODE, SUBSYSTEM_CODE, NAME, XROAD_MEMBER_ID, MEMBER_CLASS_ID, SERVER_CLIENT_ID, TYPE, ADMINISTRATIVE_CONTACT, CREATED_AT, UPDATED_AT) VALUES (702, null, 'MANAGEMENT', null, 701, null, 702, 'Subsystem', null, '2022-03-07 07:45:34.809253', '2022-03-07 07:45:34.809253');
INSERT INTO SECURITY_SERVER_CLIENTS (ID, MEMBER_CODE, SUBSYSTEM_CODE, NAME, XROAD_MEMBER_ID, MEMBER_CLASS_ID, SERVER_CLIENT_ID, TYPE, ADMINISTRATIVE_CONTACT, CREATED_AT, UPDATED_AT) VALUES (703, null, 'TEST', null, 701, null, 704, 'Subsystem', null, '2022-03-07 08:48:15.393806', '2022-03-07 08:48:15.393806');
INSERT INTO SECURITY_SERVER_CLIENTS (ID, MEMBER_CODE, SUBSYSTEM_CODE, NAME, XROAD_MEMBER_ID, MEMBER_CLASS_ID, SERVER_CLIENT_ID, TYPE, ADMINISTRATIVE_CONTACT, CREATED_AT, UPDATED_AT) VALUES (704, '222', null, 'TEST2', null, 1000001, 705, 'XRoadMember', null, '2022-03-07 08:48:15.393806', '2022-03-07 08:48:15.393806');
INSERT INTO SECURITY_SERVER_CLIENTS (ID, MEMBER_CODE, SUBSYSTEM_CODE, NAME, XROAD_MEMBER_ID, MEMBER_CLASS_ID, SERVER_CLIENT_ID, TYPE, ADMINISTRATIVE_CONTACT, CREATED_AT, UPDATED_AT) VALUES (705, '221', null, 'TEST3', null, 701, 706, 'XRoadMember', null, '2022-03-18 12:18:15.393806', '2022-03-18 12:48:15.393806');
INSERT INTO SECURITY_SERVER_CLIENTS (ID, MEMBER_CODE, SUBSYSTEM_CODE, NAME, XROAD_MEMBER_ID, MEMBER_CLASS_ID, SERVER_CLIENT_ID, TYPE, ADMINISTRATIVE_CONTACT, CREATED_AT, UPDATED_AT) VALUES (706, '223', null, 'TEST4', null, 1000001, 707, 'XRoadMember', null, '2022-03-21 12:18:15.333333', '2022-03-21 12:48:15.333333');

INSERT INTO SECURITY_SERVERS (ID, SERVER_CODE, OWNER_ID, ADDRESS, CREATED_AT, UPDATED_AT) VALUES (701, 'ADMINSS2', 701, '10.85.227.109', '2022-03-07 08:27:17.395387', '2022-03-07 08:27:17.395387');
INSERT INTO SECURITY_SERVERS (ID, SERVER_CODE, OWNER_ID, ADDRESS, CREATED_AT, UPDATED_AT) VALUES (702, 'SERVICESS2_CODE', 705, '10.85.227.239', '2022-03-15 08:27:17.222222', '2022-03-15 08:27:17.222222');
INSERT INTO SECURITY_SERVERS (ID, SERVER_CODE, OWNER_ID, ADDRESS, CREATED_AT, UPDATED_AT) VALUES (703, 'SERVICESS1_CODE', 704, '10.85.227.111', '2022-03-15 08:27:17.111111', '2022-03-15 08:27:17.111111');
INSERT INTO SECURITY_SERVERS (ID, SERVER_CODE, OWNER_ID, ADDRESS, CREATED_AT, UPDATED_AT) VALUES (704, 'SERVICESS3_CODE', 706, '10.85.227.333', '2022-03-21 08:25:17.333333', '2022-03-21 08:25:17.333333');
INSERT INTO SECURITY_SERVERS (ID, SERVER_CODE, OWNER_ID, ADDRESS, CREATED_AT, UPDATED_AT) values (1000001, 'server1', 1000001, 'server1.test', now(), now());
INSERT INTO SECURITY_SERVERS (ID, SERVER_CODE, OWNER_ID, ADDRESS, CREATED_AT, UPDATED_AT) values (1000002, 'server2', 1000004, 'server2.test', now(), now());

INSERT INTO SERVER_CLIENTS (ID, SECURITY_SERVER_ID, SECURITY_SERVER_CLIENT_ID) values (1000001, 1000001, 1000001);
INSERT INTO SERVER_CLIENTS (ID, SECURITY_SERVER_ID, SECURITY_SERVER_CLIENT_ID) values (1000002, 1000001, 1000002);
INSERT INTO SERVER_CLIENTS (ID, SECURITY_SERVER_ID, SECURITY_SERVER_CLIENT_ID) values (1000003, 1000001, 1000010);
INSERT INTO SERVER_CLIENTS (ID, SECURITY_SERVER_ID, SECURITY_SERVER_CLIENT_ID) values (1000004, 1000002, 1000004);
INSERT INTO SERVER_CLIENTS (ID, SECURITY_SERVER_ID, SECURITY_SERVER_CLIENT_ID) values (1000005, 1000002, 1000010);
INSERT INTO SERVER_CLIENTS (ID, SECURITY_SERVER_ID, SECURITY_SERVER_CLIENT_ID) VALUES (701, 701, 702);
INSERT INTO SERVER_CLIENTS (ID, SECURITY_SERVER_ID, SECURITY_SERVER_CLIENT_ID) VALUES (702, 701, 703);
INSERT INTO SERVER_CLIENTS (ID, SECURITY_SERVER_ID, SECURITY_SERVER_CLIENT_ID) VALUES (703, 702, 703);
INSERT INTO SERVER_CLIENTS (ID, SECURITY_SERVER_ID, SECURITY_SERVER_CLIENT_ID) VALUES (704, 703, 704);
