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
                Repository.savefile(filename);
                String shaID = Repository.getShaID(Repository.CWD,filename);// create a blob of adding file and return it's shaID
                // somehow add to the stage area
                Repository.addtostage(filename,shaID);
                Repository.teststagetree();
                break;
            case "test":
                Repository.teststagetree();
                break;
            case "commit":
                String msg = args[1];
                if(msg.length() == 0)
                    throw new IOException("Please enter a commit message.");
                Repository.teststagetree();
                Repository.makecommit(msg,"Yi Shen");
                break;
            case "rm":
                String rmfilename = args[1];
                Repository.removefile(rmfilename);
                break;
            case "checkout":
                if(args.length == 3){
                    Commit c = Commit.returncurrentCommit();
                    Repository.FileErrorTest(args[1], c);
                    Repository.checkout(null,args[2],null);
                    break;
                }
                else if(args.length == 4) {
                    Commit c = Commit.fromfile(args[1]);
                    Repository.CommitIDTest(args[1]);
                    Repository.FileErrorTest(args[3], c);
                    Repository.checkout(args[1], args[3], null);
                    break;
                }
                else if(args.length == 2) {
                    Repository.BranchTest(args[1]);
                    Repository.checkout(null, null, args[1]);
                    break;
                }
            // TODO: FILL THE REST IN
            default:
                Utils.error(String.format("Unknown command: %s", args[0]),args[1]);
        }
    }
}
