-- Default product edition and quota configuration.
-- This script is idempotent because docker-compose reruns init scripts on start.

INSERT INTO rpa.sys_product_version (version_code, deleted, create_time)
VALUES
    ('personal', 0, CURRENT_TIMESTAMP),
    ('professional', 0, CURRENT_TIMESTAMP),
    ('enterprise', 0, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
    deleted = VALUES(deleted);

DELETE vdc
FROM rpa.sys_version_default_config vdc
JOIN rpa.sys_product_version pv ON pv.id = vdc.version_id
WHERE pv.version_code IN ('personal', 'professional', 'enterprise')
  AND vdc.resource_code IN ('designer_count', 'market_join_count');

INSERT INTO rpa.sys_version_default_config
    (version_id, resource_code, resource_type, parent_code, default_value, url_patterns, description, deleted, create_time, update_time)
SELECT pv.id, defaults.resource_code, defaults.resource_type, defaults.parent_code,
       defaults.default_value, defaults.url_patterns, defaults.description,
       0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM rpa.sys_product_version pv
JOIN (
    SELECT 'personal' AS version_code, 'designer_count' AS resource_code, 1 AS resource_type,
           NULL AS parent_code, 19 AS default_value, '["/quota/check-designer"]' AS url_patterns,
           'Personal edition editable application quota' AS description
    UNION ALL
    SELECT 'personal', 'market_join_count', 1, NULL, 3,
           '["/quota/check-market-join"]', 'Personal edition team market quota'
    UNION ALL
    SELECT 'professional', 'designer_count', 1, NULL, 99,
           '["/quota/check-designer"]', 'Professional edition editable application quota'
    UNION ALL
    SELECT 'professional', 'market_join_count', 1, NULL, -1,
           '["/quota/check-market-join"]', 'Professional edition team market quota'
    UNION ALL
    SELECT 'enterprise', 'designer_count', 1, NULL, -1,
           '["/quota/check-designer"]', 'Enterprise edition editable application quota'
    UNION ALL
    SELECT 'enterprise', 'market_join_count', 1, NULL, -1,
           '["/quota/check-market-join"]', 'Enterprise edition team market quota'
) defaults ON defaults.version_code = pv.version_code
WHERE pv.deleted = 0;

INSERT INTO rpa.sys_tenant_config (tenant_id, version_id, extra_config_json, deleted, create_time, update_time)
SELECT '7fd5161b-4bcc-4309-b5ec-8035fcdfceeb', pv.id, '{}', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM rpa.sys_product_version pv
WHERE pv.version_code = 'personal' AND pv.deleted = 0
ON DUPLICATE KEY UPDATE
    version_id = VALUES(version_id),
    deleted = VALUES(deleted),
    update_time = CURRENT_TIMESTAMP;
