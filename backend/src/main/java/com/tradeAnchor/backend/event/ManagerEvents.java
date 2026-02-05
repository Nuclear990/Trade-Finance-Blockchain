package com.tradeAnchor.backend.event;

import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;

import java.util.List;

public final class ManagerEvents {

    private ManagerEvents() {}

    /* ===================== LCIssued ===================== */

    public static final Event LC_ISSUED = new Event(
            "LCIssued",
            List.of(
                    new TypeReference<Uint256>(true) {},  // lcId
                    new TypeReference<Uint256>(true) {},  // txnID
                    new TypeReference<Address>(true) {},  // issuerBank
                    new TypeReference<Address>() {},      // importer
                    new TypeReference<Address>() {},      // exporter
                    new TypeReference<Uint256>() {},      // amount
                    new TypeReference<Uint256>() {}       // goods
            )
    );

    /* ===================== BLIssued ===================== */

    public static final Event BL_ISSUED = new Event(
            "BLIssued",
            List.of(
                    new TypeReference<Uint256>(true) {},  // blId
                    new TypeReference<Uint256>(true) {},  // txnID
                    new TypeReference<Address>(true) {},  // shipper
                    new TypeReference<Address>() {},      // exporter
                    new TypeReference<Address>() {},      // importer
                    new TypeReference<Uint256>() {}       // goods
            )
    );

    /* ===================== Settled ===================== */

    public static final Event SETTLED = new Event(
            "Settled",
            List.of(
                    new TypeReference<Uint256>(true) {},  // txnID
                    new TypeReference<Uint256>() {},      // lcId
                    new TypeReference<Uint256>() {},      // blId
                    new TypeReference<Address>() {},      // importer
                    new TypeReference<Address>() {},      // exporter
                    new TypeReference<Uint256>() {},      // amount
                    new TypeReference<Uint256>() {}       // goods
            )
    );

    /* ===================== Topics ===================== */

    public static final String LC_ISSUED_TOPIC =
            EventEncoder.encode(LC_ISSUED);

    public static final String BL_ISSUED_TOPIC =
            EventEncoder.encode(BL_ISSUED);

    public static final String SETTLED_TOPIC =
            EventEncoder.encode(SETTLED);
}
