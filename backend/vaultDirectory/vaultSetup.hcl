storage "file" {
  path = "/opt/vault/data"
}


listener "tcp" {
  address     = "127.0.0.1:8200"
  tls_disable = 1
}


api_addr = "http://127.0.0.1:8200"


seal "transit" {
  address    = "http://127.0.0.1:8300"
  key_name   = "main-vault-unseal"
  mount_path = "transit"
}


ui = true
