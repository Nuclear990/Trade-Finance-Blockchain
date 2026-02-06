// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

interface IIdentity {
    enum Role { Bank, Company, Shipper }
    function getUser(address user)
        external
        view
        returns (Role, uint256, uint256, uint256, bool);

    function reserve(address user, uint256 amount) external;
    function release(address user, uint256 amount) external;
    function debit(address user, uint256 amount) external;
    function credit(address user, uint256 amount) external;
    function updateGoods(address user, int256 amount) external;
}

interface ILCToken {
    struct Token {
        uint256 txnID;
        address importer;
        address exporter;
        address issuerBank;
        address beneficiaryBank;
        address shipper;
        uint256 amount;
        uint256 goods;
        bool utilized;
    }

    function getToken(uint256 id) external view returns (Token memory);
    function mint(
        uint256 txnID,
        address importer,
        address exporter,
        address issuerBank,
        address beneficiaryBank,
        address shipper,
        uint256 amount,
        uint256 goods
    ) external returns (uint256);
    function markUtilized(uint256 id) external;
}

interface IBLToken {
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

    function getToken(uint256 id) external view returns (Token memory);
    function mint(
        uint256 txnID,
        address exporter,
        address importer,
        address exporterBank,
        address beneficiaryBank,
        address shipper,
        uint256 goods
    ) external returns (uint256);
    function markUtilized(uint256 id) external;
}

contract Manager {
    IIdentity public identity;
    ILCToken public lc;
    IBLToken public bl;
    address public manager; // faucet

    modifier onlyManager() {
        require(msg.sender == manager, "Only manager");
        _;
    }

/* ---------- EVENTS ---------- */

event LCIssued(
    uint256 indexed lcId,
    uint256 indexed txnID,
    address indexed issuerBank,
    address importer,
    address exporter,
    uint256 amount,
    uint256 goods
);

event BLIssued(
    uint256 indexed blId,
    uint256 indexed txnID,
    address indexed shipper,
    address exporter,
    address importer,
    uint256 goods
);

event Settled(
    uint256 indexed txnID,
    uint256 lcId,
    uint256 blId,
    address importer,
    address exporter,
    uint256 amount,
    uint256 goods
);


    constructor(address id, address lcAddr, address blAddr) {
        identity = IIdentity(id);
        lc = ILCToken(lcAddr);
        bl = IBLToken(blAddr);
        manager = msg.sender;
    }

    /* ---------- LC ---------- */

    function issueLC(
        address bankActor,
        uint256 txnID,
        address importer,
        address exporter,
        address issuerBank,
        address beneficiaryBank,
        address shipper,
        uint256 amount,
        uint256 goods
    ) external onlyManager{

    (IIdentity.Role role,,,, bool exists) = identity.getUser(bankActor);

    require(exists && role == IIdentity.Role.Bank, "Invalid bank");

    require(bankActor == issuerBank, "Issuer mismatch");

    identity.reserve(importer, amount);

        uint256 lcid = lc.mint(
            txnID,
            importer,
            exporter,
            issuerBank,
            beneficiaryBank,
            shipper,
            amount,
            goods
        );
        emit LCIssued(
    lcid,
    txnID,
    issuerBank,
    importer,
    exporter,
    amount,
    goods
);
    }

    /* ---------- BL ---------- */

    function issueBL(
        address shipperActor,
        uint256 txnID,
        address importer,
        address exporter,
        address beneficiaryBank,
        address exporterBank,
        address shipper,
        uint256 goods
    ) external onlyManager{

    (IIdentity.Role role,,,, bool exists) = identity.getUser(shipperActor);

    require(exists && role == IIdentity.Role.Shipper, "Invalid shipper");

    identity.updateGoods(exporter, -int256(goods));

    identity.updateGoods(shipper, int256(goods));

        uint256 blid = bl.mint(
            txnID,
            exporter,
            importer,
            exporterBank,
            beneficiaryBank,
            shipper,
            goods
        );
        emit BLIssued(
    blid,
    txnID,
    shipper,
    exporter,
    importer,
    goods
);
    }

    

    /* ---------- SETTLEMENT ---------- */

    function settle(
        address exporterBankActor,
        uint256 lcId,
        uint256 blId
    ) external onlyManager{

    (IIdentity.Role role,,,, bool exists) = identity.getUser(exporterBankActor);

    require(exists && role == IIdentity.Role.Bank, "Invalid bank");

    ILCToken.Token memory lcT = lc.getToken(lcId);

    IBLToken.Token memory blT = bl.getToken(blId);

    require(!lcT.utilized && !blT.utilized, "Already settled");

    require(lcT.txnID == blT.txnID, "Txn mismatch");

    require(lcT.goods == blT.goods, "Goods mismatch");

    require(lcT.beneficiaryBank == blT.exporterBank, "Bank mismatch");

    /* ---- money: importer → exporter ---- */
    identity.release(lcT.importer, lcT.amount);

    identity.debit(lcT.importer, lcT.amount);

    identity.credit(lcT.exporter, lcT.amount);

    /* ---- goods: shipper → importer ---- */
    identity.updateGoods(blT.shipper, -int256(blT.goods));

    identity.updateGoods(blT.importer, int256(blT.goods));

    lc.markUtilized(lcId);

    bl.markUtilized(blId);


        emit Settled(
    lcT.txnID,
    lcId,
    blId,
    lcT.importer,
    lcT.exporter,
    lcT.amount,
    lcT.goods
);

    }
}
