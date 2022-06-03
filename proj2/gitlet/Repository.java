package gitlet;


import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

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
        File storefile;
        if(shaID == null) // handle null like writing the head
            storefile = storepath;
        else
            storefile = Utils.join(storepath, shaID);// where to write in the data (blobs, stage, commit..) filename is shaID
        //creatfile(storefile);
        writeObject(storefile, items);
    }

    public static void writein(Staging items, File storepath, String shaID){
        File storefile;
        if(shaID == null) // handle null like writing the head
            storefile = storepath;
        else
            storefile = Utils.join(storepath, shaID);// where to write in the data (blobs, stage, commit..) filename is shaID
        //creatfile(storefile);
        writeObject(storefile, items);
    }
    public static void teststagetree(){
        Staging stage = readObject(INDEX,Staging.class);
        TreeMap addtree = stage.getAddedFiles();
        System.out.println(addtree);
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
        TreeMap<String, String> addtree = CurrentStage.getAddedFiles();
        ArrayList<String> rmlist = CurrentStage.getRemovedFiles();
        //teststagetree();
        if(addtree.isEmpty() && rmlist.isEmpty())
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
    public static void savefile(String filename) throws IOException {
        File storepath = blobs_DIR;
        String shaID = getShaID(CWD, filename);
        File testfile = Utils.join(storepath,shaID);// to test if the current blobs already exist
        if(!testfile.exists()){
            String items = getItem(join(CWD, filename));
            writein(items, storepath,shaID);
        }
    }
    public static String getItem(File file){
        return readContentsAsString(file);
    }
    public static String getShaID(File path, String filename) throws IOException {
        File f = Utils.join(path,filename);
        if(!f.exists()){
            throw new IOException("File does not exist.");
        }
        String items = getItem(f);
        return Utils.sha1(items);
    }
    /** return the ShaID of current commit's sha-1 ID(i.e. the parent of next commit) */
    public static String returnparentID(){
        String currenthead = Utils.readObject(HEAD, String.class);
        File headCommit = Utils.join(heads_DIR,currenthead);
        return Utils.readObject(headCommit, String.class);
    }
    public static boolean IsinParentCommit(String filename, String shaID){
        String parentID = Repository.returnparentID(); // parent for next commit is current commit
        File parentfile = Utils.join(Repository.blobs_DIR, parentID);
        Commit parent = Utils.readObject(parentfile,Commit.class);
        if(IsSameFile(parent.Committree,filename,shaID)) // all same for filename and blobs
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

        //boolean c = IsinParentCommit(filename, shaID);
        //if (c)
           // Stage.remove(filename);
        // remove it from the staging area if it is already there (as can happen when a file is changed, added, and then changed back to it’s original version)
        // if c = 0 means filename and contents doesn't change in this version, so do not add
        writeObject(INDEX, Stage);
        // Storing stage
    }
    public static void removefile(String filename) throws IOException {
        Staging Stage = Utils.readObject(INDEX, Staging.class);
        TreeMap<String, String> add = Stage.getAddedFiles();
        String sha1 =  add.get(filename);
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
    public static boolean checkisnull(String str){
        return str == null ;
    }
    /** overwrite or put the file tracked in the commit to the path */
    public static void overwritefile(String commitID, String filename, File path){
        Commit c = Commit.fromfile(commitID);
        System.out.println(c.Committree);
        String fileshaID = c.Committree.get(filename);
        System.out.println(fileshaID);
        File file = Utils.join(blobs_DIR,fileshaID);
        String content = readObject(file, String.class);
        writein(content,path,filename);
    }
    /** test if that filename exist in c*/
    public static boolean IsFilenameExist(String filename, Commit c){
        return c.Committree.containsKey(filename);
    }
    public static void FileErrorTest(String filename, Commit c) throws IOException {
        if(IsFilenameExist(filename,c)){
            throw new IOException("File does not exist in that commit.");
        }
    }
    public static void CommitIDTest(String CommitID) throws IOException {
        File commit = join(blobs_DIR,CommitID);
        if(!commit.exists()){
            throw new IOException("No commit with that id exists.");
        }
    }
    public static void BranchTest(String branch) throws IOException {
        File branchfile = join(heads_DIR,branch);
        String head = readObject(HEAD, String.class);
        if(!branchfile.exists()){
            throw new IOException("No such branch exists.");
        }
        else if(branch == head){
            throw new IOException("No need to checkout the current branch.");
        }
        else if(IsExistUntrackedFile()){
            throw new IOException("There is an untracked file in the way; delete it, or add and commit it first.");
        }
    }
    /** Is this file with the same content exist inside the tree*/
    public static boolean IsSameFile(TreeMap t, String filename, String ShaID){
        return t.containsKey(filename) && (t.get(filename) == ShaID);
     }
     /** Is this a untracked file, if it is return true*/
    public static boolean IsUntrackedFile(String filename, String ShaID){
        Commit CurrentCommit = Commit.returncurrentCommit();
        boolean IsFilenameinCurrentCommit = IsFilenameExist(filename, CurrentCommit);// Is this file name exist in current commit
        Staging CurrentStage = readObject(INDEX, Staging.class);
        TreeMap<String, String> addtree = CurrentStage.getAddedFiles();
        ArrayList<String> rmlist = CurrentStage.getRemovedFiles();
        boolean IsinAddtree = IsSameFile(addtree,filename,ShaID); // Is already add to stage
        boolean IsinDellist = rmlist.contains(filename); // Is already stage to rm
        return IsFilenameinCurrentCommit || IsinAddtree || IsinDellist;
    }
    /** if there exist a untracked file in CWD return true, else return false */
    public static boolean IsExistUntrackedFile(){
        List<String> CWDFile = plainFilenamesIn(CWD);
        for(String filename : CWDFile){
            File file = join(CWD,filename);
            String ShaID = sha1(readObject(file, String.class));
            if(IsUntrackedFile(filename, ShaID)){
                return true;
            }
        }
        return false;
    }
    /** three case for checkout cmd */
    public static void checkout(String commitID, String filename, String branch){
        if(checkisnull(branch)) {
            if (!checkisnull(filename) && checkisnull(commitID)) {
                overwritefile(returnparentID(), filename, CWD);
            }
            else if (!checkisnull(commitID)) {
                overwritefile(commitID, filename, CWD);
            }
        }
        else{
            Commit currentcommit = Commit.returncurrentCommit();
            TreeMap<String, String> CurrentCommittree = currentcommit.Committree;
            File branchfile = join(heads_DIR,branch);
            String branchshaID = readObject(branchfile, String.class);
            Commit branchcommit = Commit.fromfile(branchshaID);
            TreeMap<String, String> branchcommittree = branchcommit.Committree;
            Set<Map.Entry<String, String>> trackedFilePairs = branchcommittree.entrySet();// entry of branch committree
            Set<Map.Entry<String, String>> CurrentFilePairs = CurrentCommittree.entrySet();// entry of Current committree
            for(Map.Entry<String, String> filepair : trackedFilePairs){
                String branchfilename = filepair.getKey();
                String branchfileShaID = filepair.getValue();
                overwritefile(branchfileShaID, branchfilename, CWD);// put the file tracked in branch head commit to CWD
            }
            for(Map.Entry<String, String> filepair : CurrentFilePairs){
                String Currentfilename = filepair.getKey();
                String CurrentfileShaID = filepair.getValue();
                if(!IsSameFile(branchcommittree,Currentfilename,CurrentfileShaID)){
                    rmfilefromCWD(Currentfilename);}// del the file that is track in current commit but not in branch head
            }
        }
    }
    public void creatNewBranch(String branchname){
        //TODO;
    }
}
