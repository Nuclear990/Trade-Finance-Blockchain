// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC721/ERC721.sol";

contract BLToken is ERC721 {
    struct Token {
        uint256 txnID;
        address exporter;
        address importer;
        address exporterBank;
        address beneficiaryBank;
        address shipper;
        uint256 goods;
        bool utilized;
    }

    mapping(uint256 => Token) private tokens;
    uint256 public nextId = 1;
    address public manager;

    modifier onlyManager() {
        require(msg.sender == manager, "Only manager");
        _;
    }

    constructor() ERC721("Bill of Lading", "BL") {
        manager = msg.sender;
    }

    function setManager(address newManager) external onlyManager {
        manager = newManager;
    }

    function mint(
        uint256 txnID,
        address exporter,
        address importer,
        address exporterBank,
        address beneficiaryBank,
        address shipper,
        uint256 goods
    ) external onlyManager returns (uint256) {
        uint256 id = nextId++;

        tokens[id] = Token(
            txnID,
            exporter,
            importer,
            exporterBank,
            beneficiaryBank,
            shipper,
            goods,
            false
        );

        _mint(exporterBank, id);
        return id;
    }



    function markUtilized(uint256 tokenId) external onlyManager {
        tokens[tokenId].utilized = true;
    }

    function getToken(uint256 tokenId) external view returns (Token memory) {
        return tokens[tokenId];
    }
}
