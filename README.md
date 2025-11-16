**Trade Finance Blockchain — Hyperledger Besu (IBFT) Prototype**

This project is a complete prototype of an international trade finance workflow built on a private blockchain network using Hyperledger Besu with IBFT consensus. It demonstrates how blockchain can digitize and automate the movement of key trade documents such as the Letter of Credit (LC) and Bill of Lading (B/L), while coordinating actions between banks, companies, and shippers.

The project models a realistic end-to-end trade process while remaining simple enough for learning, academic use, and interviews.

**Project Overview**

The system tracks the major steps of a cross-border trade:

  Company requests a Letter of Credit.
  
  Bank issues the LC as an ERC-721 token.
  
  Shipper issues the Bill of Lading as an ERC-721 token.
  
  Exporter submits the B/L to the bank.
  
  Bank verifies the B/L against LC conditions.
  
  Smart contract automatically executes payment using Ether.
  
  Ownership of the B/L token transfers to the importer.
  
  Importer presents the B/L to the shipper to receive goods.
  
  This ensures that documents cannot be forged, conditions are enforced by smart contracts, and all activity is transparent and auditable.

**On-Chain Architecture**

The blockchain layer contains four core smart contracts:

  IdentityRegistry
  Stores authorized participants: banks, companies, and shippers.
  
  LCToken (ERC-721)
  Represents the Letter of Credit. Stores applicant (importer), beneficiary (exporter), LC amount, and terms.
  
  BLToken (ERC-721)
  Represents the Bill of Lading. Stores shipper, exporter, importer, and goods-related metadata.
  
  TradeFinanceManager
  Validates LC and B/L, triggers payment, transfers B/L ownership, and authorizes release of goods.

**Off-Chain Architecture**

The web application is built using:

  -- Spring Boot for backend services and REST APIs.
  -- Web3j for interacting with the Besu nodes and smart contracts.
  -- PostgreSQL for local persistence and fast querying.
  -- HTML, CSS, and JavaScript for front-end dashboards.
  -- Ethers.js for wallet interactions.
  
  Off-chain tables mirror key on-chain data such as LC details, B/L details, participant information, balances, and logs to provide cleaner UI dashboards and faster lookups.

**System Actors**

  **Bank**
    Issues LC, verifies B/L, releases or receives payments.
  
  **Company**
    Acts as importer or exporter depending on the transaction.
  
  **Shipper**
    Issues B/L and releases the goods to the importer.
    Roles for a company are determined per transaction instead of being tied permanently to the entity.

**Key Features**

  Private blockchain network using IBFT consensus.
  
  ERC-721 tokenization of LC and B/L documents.
  
  Smart-contract-based payment automation using Ether.
  
  Role-based web dashboards for banks, companies, and shippers.
  
  Goods release controlled by ownership of the B/L token.
  
  Full audit trail both on-chain and off-chain.
  
  End-to-end simulation of a real trade finance process.
