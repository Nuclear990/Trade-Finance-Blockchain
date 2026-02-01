vault {
  address = "http://127.0.0.1:8200"
}

auto_auth {
  method "approle" {
    mount_path = "auth/approle"
    config = {
      role_id_file_path   = "/home/catpuccino/vaultIds/approle/role_id"
      secret_id_file_path = "/home/catpuccino/vaultIds/approle/secret_id"
    }
  }

  sink "file" {
    config = {
      path = "/home/catpuccino/vaultIds/agent-token"
    }
  }
}

