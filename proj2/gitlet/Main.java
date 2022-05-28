package gitlet;

import java.io.IOException;
import gitlet.Utils.*;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) throws IOException {
        // TODO: what if args is empty?
        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                // TODO: handle the `init` command
                Repository.initial_folder();
                Commit.initialize("Yi Shen");
                break;
            case "add":
                // TODO: handle the `add [filename]` command
                String filename = args[1];
                String shaID = Repository.savefile(filename);// create a blob of adding file and return it's shaID
                // somehow add to the stage area
                Repository.addtostage(filename,shaID);
                break;
            case "commit":
                String msg = args[1];
                if(msg.length() == 0)
                    throw new IOException("Please enter a commit message.");
                Repository.makecommit(msg,"Yi Shen");
            case "rm":
                String rmfilename = args[1];
                Repository.removefile(rmfilename);
            // TODO: FILL THE REST IN
            default:
                Utils.error(String.format("Unknown command: %s", args[0]),args[1]);
        }
    }
}
