package gitlet;

import java.io.File;
import java.io.IOException;
import gitlet.Utils.*;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Yi Shen
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) throws IOException {
        if(args[0].equals(null)){
            System.out.println("Please enter the cmd");
            System.exit(0);
        }
        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                Repository.initial_folder();
                Commit.initialize("Yi Shen");
                break;
            case "add":
                String filename = args[1];
                Repository.savefile(filename);
                String shaID = Repository.getShaID(Repository.CWD,filename);// create a blob of adding file and return it's shaID
                // somehow add to the stage area
                Repository.addtostage(filename,shaID);
                //Repository.teststagetree();
                break;
            case "test":
                Repository.teststagetree();
                break;
            case "commit":
                String msg = args[1];
                if(msg.length() == 0)
                    throw new IOException("Please enter a commit message.");
                //Repository.teststagetree();
                Repository.makecommit(msg,"Yi Shen");
                break;
            case "rm":
                String rmfilename = args[1];
                Repository.removefile(rmfilename);
                break;
            case "log":
                String CurrentCommitID = Repository.returnparentID();
                System.out.println(Repository.history(CurrentCommitID));
                break;
            case "status":
                String status = Repository.StatusInterface();
                System.out.println(status);
                break;
            case "checkout":
                if(args.length == 3){
                    Commit c = Commit.returncurrentCommit();
                    Repository.FileErrorTest(args[2], c);
                    Repository.checkout(null,args[2],null);
                    break;
                }
                else if(args.length == 4) {
                    Commit c = Commit.fromfile(args[1]);
                    System.out.println(c.Committree);
                    Repository.CommitIDTest(args[1]);
                    Repository.FileErrorTest(args[3], c);
                    Repository.checkout(args[1], args[3], null);
                    break;
                }
                else if(args.length == 2) {
                    String Branchname = args[1];
                    String BranchID = Repository.getBranchID(Branchname);
                    Repository.BranchTest(Branchname);
                    Repository.checkout(null, null, BranchID);
                    Commit.setHEAD(BranchID, Branchname);
                    Repository.cleanstage();
                    break;
                }
            case "reset":
                String CommitID = args[1];
                File Commitfile = Utils.join(Repository.blobs_DIR,CommitID);
                if(!Commitfile.exists()){
                    System.out.println("No commit with that id exists.");
                    System.exit(0);
                }
                else if(Repository.IsExistUntrackedFile()){
                    System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                    System.exit(0);
                }
                String CurrentBranchHead = Repository.getHEAD();
                Repository.checkout(null,null, CommitID);
                Commit.setHEAD(CommitID,CurrentBranchHead);
                Repository.cleanstage();
                break;
            case "branch":
                String newBranchName = args[1];
                if(Repository.IsBranchNameExist(newBranchName)){
                    System.out.println("A branch with that name already exists.");
                    System.exit(0);
                }
                Repository.creatNewBranch(newBranchName);
                break;
            case "stageclear":
                Repository.cleanstage();
                break;
            case "rm-branch":
                String Branchname = args[1];
                Repository.delbranch(Branchname);
                break;
            case "merge":
                String GivenBranch = args[1];
                if(!Repository.IsBranchNameExist(GivenBranch)){
                    System.out.println("A branch with that name does not exist.");
                    System.exit(0);
                };
                if(Repository.IsExistUntrackedFile()){
                    System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                    System.exit(0);
                }
                Repository.StageClearTest();
                Repository.merge(GivenBranch);
                break;
            default:
                Utils.error(String.format("Unknown command: %s", args[0]),args[1]);
        }
    }
}
