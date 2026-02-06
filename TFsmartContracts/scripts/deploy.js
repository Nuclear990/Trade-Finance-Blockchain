const hre = require("hardhat");

async function main() {
    console.log("=== DEPLOY SCRIPT STARTED ===");

    const [deployer] = await hre.ethers.getSigners();

    console.log("Deployer:", deployer.address);
    console.log(
        "Balance:",
        hre.ethers.formatEther(
            await deployer.provider.getBalance(deployer.address)
        ),
        "ETH"
    );

    /* ------------------------------------------------ */
    /* 1. Deploy Identity                               */
    /* ------------------------------------------------ */

    const IdentityFactory = await hre.ethers.getContractFactory("Identity");
    const identity = await IdentityFactory.deploy();
    await identity.waitForDeployment();

    const identityAddr = await identity.getAddress();
    console.log("Identity deployed at:", identityAddr);

    /* ------------------------------------------------ */
    /* 2. Deploy LCToken                                */
    /* ------------------------------------------------ */

    const LCTokenFactory = await hre.ethers.getContractFactory("LCToken");
    const lc = await LCTokenFactory.deploy();
    await lc.waitForDeployment();

    const lcAddr = await lc.getAddress();
    console.log("LCToken deployed at:", lcAddr);

    /* ------------------------------------------------ */
    /* 3. Deploy BLToken                                */
    /* ------------------------------------------------ */

    const BLTokenFactory = await hre.ethers.getContractFactory("BLToken");
    const bl = await BLTokenFactory.deploy();
    await bl.waitForDeployment();

    const blAddr = await bl.getAddress();
    console.log("BLToken deployed at:", blAddr);

    /* ------------------------------------------------ */
    /* 4. Deploy Manager                                */
    /* ------------------------------------------------ */

    const ManagerFactory = await hre.ethers.getContractFactory("Manager");
    const manager = await ManagerFactory.deploy(
        identityAddr,
        lcAddr,
        blAddr
    );
    await manager.waitForDeployment();

    const managerAddr = await manager.getAddress();
    console.log("Manager deployed at:", managerAddr);

    /* ------------------------------------------------ */
    /* 5. Hand over authority to Manager                */
    /* ------------------------------------------------ */

    console.log("Wiring managers...");

    let tx;

    tx = await identity.setManager(managerAddr);
    await tx.wait();
    console.log("✓ Identity.manager = Manager");

    tx = await lc.setManager(managerAddr);
    await tx.wait();
    console.log("✓ LCToken.manager = Manager");

    tx = await bl.setManager(managerAddr);
    await tx.wait();
    console.log("✓ BLToken.manager = Manager");

    console.log("=== DEPLOY COMPLETE ===");
    console.log("");
    console.log("SAVE THESE ADDRESSES:");
    console.log("IDENTITY =", identityAddr);
    console.log("LC_TOKEN =", lcAddr);
    console.log("BL_TOKEN =", blAddr);
    console.log("MANAGER  =", managerAddr);
}

main().catch((error) => {
    console.error("DEPLOY FAILED:", error);
    process.exit(1);
});
