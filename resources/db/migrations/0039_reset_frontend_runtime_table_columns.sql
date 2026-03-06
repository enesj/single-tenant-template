DELETE FROM frontend_runtime_configs
WHERE config_key = 'table-columns'
  AND scope IN ('admin', 'user');
