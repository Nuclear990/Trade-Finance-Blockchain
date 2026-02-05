# REQUIRED by Spring Vault
path "auth/token/lookup-self" {
  capabilities = ["read"]
}

# KV v2 data operations (actual secrets)
path "eth-keys/data/*" {
  capabilities = ["create", "read", "update", "delete"]
}

# KV v2 metadata access (listing + existence checks)
path "eth-keys/metadata/*" {
  capabilities = ["read", "list"]
}
