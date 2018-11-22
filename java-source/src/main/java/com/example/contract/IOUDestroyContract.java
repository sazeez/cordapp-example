package com.example.contract;

import com.example.state.IOUState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.identity.AbstractParty;
import net.corda.core.transactions.LedgerTransaction;

import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

/**
 * A implementation of a basic smart contract in Corda.
 * <p>
 * This contract enforces rules regarding the cancellation of a valid [IOUState], which in turn encapsulates an [IOU].
 * <p>
 * For existing [IOU] to be cancelled from the ledger, a transaction is required which takes:
 * - Existing IOU state.
 * - No output state.
 * - A Destroy() command with the public keys of both the lender and the borrower.
 * <p>
 * All contracts must sub-class the [Contract] interface.
 */
public class IOUDestroyContract implements Contract {
    public static final String IOU_CONTRACT_ID = "com.example.contract.IOUDestroyContract";

    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
    @Override
    public void verify(LedgerTransaction tx) {
        final CommandWithParties<Commands.Destroy> command = requireSingleCommand(tx.getCommands(), Commands.Destroy.class);
        requireThat(require -> {
            // Generic constraints around the IOU transaction.
            final IOUState inp = tx.inputsOfType(IOUState.class).get(0);
            require.using("The inputs consumed must be an IOU.",
                    inp instanceof IOUState);
            require.using("No output state should be created.",
                    tx.getOutputs().size() == 0);
            require.using("Only one input state should be consumed.",
                    tx.getInputs().size() == 1);
//            final IOUState out = tx.outputsOfType(IOUState.class).get(0);
            require.using("The lender and the borrower cannot be the same entity.",
                    inp.getLender() != inp.getBorrower());
            require.using("All of the participants must be signers.",
                    command.getSigners().containsAll(inp.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList())));

            // IOU-specific constraints.
            require.using("The IOU's value must be non-negative.",
                    inp.getValue() > 0);

            return null;
        });
    }

    /**
     * This contract only implements one command, Create.
     */
    public interface Commands extends CommandData {
        class Destroy implements Commands {
        }
    }
}