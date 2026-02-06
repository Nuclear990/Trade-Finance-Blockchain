// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract Identity {
    enum Role { Bank, Company, Shipper }

    struct User {
        Role role;
        uint256 balance;
        uint256 reserved;
        uint256 goods;
        bool exists;
    }

    mapping(address => User) private users;
    address public manager;

    modifier onlyManager() {
        require(msg.sender == manager, "Only manager");
        _;
    }

    constructor() {
        manager = msg.sender; // faucet
    }

    function setManager(address newManager) external onlyManager {
        manager = newManager;
    }

    /* ---------- Users ---------- */

    function createUser(address user, Role role) external {
        require(!users[user].exists, "User exists");
        users[user] = User(role, 100, 0, 10, true);
    }

    function getUser(address user)
        external
        view
        returns (Role, uint256, uint256, uint256, bool)
    {
        User memory u = users[user];
        return (u.role, u.balance, u.reserved, u.goods, u.exists);
    }

    function availableBalance(address user) public view returns (uint256) {
        return users[user].balance - users[user].reserved;
    }

    /* ---------- Money ---------- */

    function credit(address user, uint256 amount) external onlyManager {
        require(users[user].exists, "Unknown user");
        users[user].balance += amount;
    }

    function debit(address user, uint256 amount) external onlyManager {
        require(users[user].exists, "Unknown user");
        require(availableBalance(user) >= amount, "Insufficient balance");
        users[user].balance -= amount;
    }

    function reserve(address user, uint256 amount) external onlyManager {
        require(availableBalance(user) >= amount, "Insufficient available");
        users[user].reserved += amount;
    }

    function release(address user, uint256 amount) external onlyManager {
        require(users[user].reserved >= amount, "Insufficient reserved");
        users[user].reserved -= amount;
    }

    /* ---------- Goods ---------- */

    function updateGoods(address user, int256 amount) external onlyManager {
        require(users[user].exists, "Unknown user");

        if (amount < 0) {
            require(users[user].goods >= uint256(-amount), "Insufficient goods");
            users[user].goods -= uint256(-amount);
        } else {
            users[user].goods += uint256(amount);
        }
    }
}
