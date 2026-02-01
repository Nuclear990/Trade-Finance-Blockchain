# REQUIRED by Spring Vault
path "auth/token/lookup-self" {
  capabilities = ["read"]
}

# KV v2 data operations
path "eth-keys/data/*" {
  capabilities = ["create", "read", "update", "delete", "list"]
}

# KV v2 metadata access (mandatory)
path "eth-keys/metadata/*" {
  capabilities = ["read", "list"]
}
