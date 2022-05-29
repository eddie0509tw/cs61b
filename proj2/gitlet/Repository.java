package gitlet;

import org.apache.commons.math3.analysis.function.Add;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static gitlet.Utils.*;

// TODO: any imports you need here

/** Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Repository<T extends Serializable>{
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    public static final File heads_DIR = join(GITLET_DIR, "heads"); //Storing every branches(shaID of commit)
    public static final File blobs_DIR = join(Repository.GITLET_DIR, "blobs");
    public static final File HEAD = join(GITLET_DIR,"HEAD"); //what is the current head (String indicate head inside)
    public static final File INDEX = join(GITLET_DIR,"INDEX");// stage area
    static Staging Stagearea = new Staging();
    /* TODO: fill in the rest of this class. */
    public static void initial_folder()  {
        GITLET_DIR.mkdir();
        heads_DIR.mkdir();
        blobs_DIR.mkdir();
        creatfile(HEAD);
        creatfile(INDEX);
        writein(Stagearea,INDEX,null); // create first new empty stagearea
    }
    public static void creatfile(File f){
        try {
            f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /** Write the <item> into the storing file that the path is the <storepath>,
     * the file's name is based on it's sha ID */
    public static <T extends Serializable> void writein(T items, File storepath, String shaID){
        File storefile = Utils.join(storepath, shaID);// where to write in the data (blobs, stage, commit..) filename is shaID
        if(shaID == null) // handle null like writing the head
            storefile = storepath;
        creatfile(storefile);
        writeObject(storefile, items);
    }
    /** overwrite the tree in the index with new stage */
    public static void cleanstage(){
        Staging s = readObject(INDEX, Staging.class);
        s.clear();
        writein(s, INDEX, null);
    }

    /** make a new commit
    * note that make commit will not change branch */
    public static void makecommit(String msg, String author) throws IOException {
        String parentID = Repository.returnparentID();
        String CurrentBranch = readObject(HEAD, String.class) ;
        Commit c = new Commit(msg, parentID);
        Commit parent = Commit.fromfile(parentID);
        c.CommitID  = Utils.sha1(Commit.CommittoListString(msg, author,c.getDate(),parentID));
        Staging CurrentStage = readObject(INDEX,Staging.class);
        TreeMap addtree = CurrentStage.getAddedFiles();
        ArrayList<String> rmlist = CurrentStage.getRemovedFiles();
        if(addtree.size() == 0 && rmlist.size() == 0)
            throw new IOException("No changes added to the commit.");
        Commit.setHEAD(c.CommitID, CurrentBranch);
        c.Committree = parent.Committree;
        for(String key : rmlist){
            c.Committree.remove(key);// rm the file tracking in parent commit
        }
        Set<Map.Entry<String,String>> AddEntries = addtree.entrySet();// get the set of all pairs in the commit tree
        for(Map.Entry<String,String> entry : AddEntries){
            c.Committree.put(entry.getKey(),entry.getValue());// add the stage-to-add files to Commit tree
        }
        c.savecommit();
        cleanstage();
    }

    /** save the file in the CWD to the blobs and return it's Sha-1 ID,
     * if already exist this content then do not creat file and return null*/
    public static String savefile(String filename) throws IOException {
        File f = Utils.join(CWD,filename);
        File storepath = blobs_DIR;
        if(!f.exists()){
            throw new IOException("File does not exist.");
        }
        String items = readObject(f,String.class);
        String shaID = Utils.sha1(items);
        File testfile = Utils.join(storepath,shaID);// to test if the current blobs already exist
        if(!testfile.exists()){
            writein(items, storepath,shaID);
            return shaID;
        }
        else return null;
    }
    /** return the ShaID of current commit's sha-1 ID(i.e. the parent of next commit) */
    public static String returnparentID(){
        File h = HEAD;
        String currenthead = Utils.readObject(h, String.class);
        File headCommit = Utils.join(heads_DIR,currenthead);
        String parentshaID = Utils.readObject(headCommit, String.class);
        return parentshaID;
    }
    public static boolean IsinParentCommit(String filename, String shaID){
        String parentID = Repository.returnparentID(); // parent for next commit is current commit
        File parentfile = Utils.join(Repository.blobs_DIR, parentID);
        Commit parent = Utils.readObject(parentfile,Commit.class);
        boolean IsFileName = parent.Committree.containsKey(filename);
        boolean IsContent = parent.Committree.containsValue(shaID);
        if(IsFileName && IsContent) // all same for filename and blobs
            return true;
        else return false;
        /*else if (!IsContent) // { !IsContent || !(IsFileName || IsContent) } either same filename but diff in contents or all diff
            return 1;
        else // diff in filename but same contents(blobs)
            return -1;*/
    }
    /** add the file to the current stage */
    public static void addtostage(String filename, String shaID) {//this shaID is the ID of blobs
        Staging Stage = Utils.readObject(Repository.INDEX, Staging.class);
        /*if(Stage.size() == 0){
            Stage = Stagearea.getAddedFiles();
            Commit CurrentCommit = returncurrentCommit();
            Stage = CurrentCommit.Committree;
        }*/
        Stage.add(filename, shaID);
        boolean c = IsinParentCommit(filename, shaID);
        if (c)
            Stage.remove(filename);
        // remove it from the staging area if it is already there (as can happen when a file is changed, added, and then changed back to it’s original version)
        // if c = 0 means filename and contents doesn't change in this version, so do not add
        writein(Stage, INDEX,null);
        // Storing stage
    }
    public static void removefile(String filename) throws IOException {
        Staging Stage = Utils.readObject(INDEX, Staging.class);
        TreeMap add = Stage.getAddedFiles();
        String sha1 = (String) add.get(filename);
        boolean c = IsinParentCommit(filename,sha1);
        if(c){
            Stage.addToRemovedFiles(filename);//if track in parent commit, rm from CWD and stage for remove
            rmfilefromCWD(filename);
        }
        else if (add.containsKey(filename)) {//if it is stage for addidtion, unstage it
            add.remove(filename);
        }
        else throw new IOException("No reason to remove the file.");
        writein(Stage, INDEX,null);
    }
    /** delete file from CWD */
    public static void rmfilefromCWD(String filename){
        File rmfile = join(CWD,filename);
        if(rmfile.exists()){
            restrictedDelete(rmfile);}
    }
    public void creatNewBranch(String branchname){
        //TODO;
    }
}
