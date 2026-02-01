storage "file" {
  path = "/opt/vault-unseal/data"
}

listener "tcp" {
  address     = "127.0.0.1:8300"
  tls_disable = 1
}

api_addr = "http://127.0.0.1:8300"
ui = false
