package gitlet;


import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.Queue;
import java.util.LinkedList;


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
    /** create a new file given a file obj */
    public static void creatfile(File f){
        try {
            f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /** return the HEAD you current at */
    public static String getHEAD(){
        String CurrentHead = readContentsAsString(HEAD);
        return CurrentHead;
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
    public static void writein(String items, File storepath, String shaID){
        File storefile;
        if(shaID == null) // handle null like writing the head
            storefile = storepath;
        else
            storefile = Utils.join(storepath, shaID);// where to write in the data (blobs, stage, commit..) filename is shaID
        //creatfile(storefile);
        writeContents(storefile, items);
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
    public static void makecommit(String msg, String author) {
        String parentID;
        parentID = Repository.returnparentID();
        String CurrentBranch = readContentsAsString(HEAD);
        System.out.println("parentID is: " + parentID);
        Commit c = new Commit(msg, parentID);
        Commit parent = Commit.fromfile(parentID);
        c.CommitID  = Utils.sha1(Commit.CommittoListString(msg, author,c.getDate(),parentID));
        //System.out.println(c.CommitID);
        Staging CurrentStage = readObject(INDEX,Staging.class);
        TreeMap<String, String> addtree = CurrentStage.getAddedFiles();
        ArrayList<String> rmlist = CurrentStage.getRemovedFiles();
        if(addtree.isEmpty() && rmlist.isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
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
    /** Merge version of make commit */
    public static void makecommit(String msg, String author,String parent1ID, String parent2ID) {
        //String parentID = String.format(parent1ID.substring(0,5)+" "+parent2ID.substring(0,5));
        String parentID = "two parent";
        String CurrentBranch = readContentsAsString(HEAD);
        Commit c = new Commit(msg, parent1ID);
        c.addparentID(parent2ID);
        Commit parent = Commit.returncurrentCommit();
        List<String> parentlist = c.getParentIDList();
        c.CommitID  = Utils.sha1(Commit.CommittoListString(msg, author,c.getDate(),parentID));
        //System.out.println(c.CommitID);
        Staging CurrentStage = readObject(INDEX,Staging.class);
        TreeMap<String, String> addtree = CurrentStage.getAddedFiles();
        ArrayList<String> rmlist = CurrentStage.getRemovedFiles();
        if(addtree.isEmpty() && rmlist.isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
        Commit.setHEAD(c.CommitID, CurrentBranch);
        c.Committree = parent.Committree;
        System.out.println("The remove list is now : " + rmlist);
        for(String key : rmlist){
            c.Committree.remove(key);// rm the file tracking in parent commit
            rmfilefrom(key,CWD);
        }
        Set<Map.Entry<String,String>> AddEntries = addtree.entrySet();// get the set of all pairs in the commit tree
        for(Map.Entry<String,String> entry : AddEntries){
            c.Committree.put(entry.getKey(),entry.getValue());// add the stage-to-add files to Commit tree
            putFileTo(entry.getKey(),entry.getValue(),CWD);
        }
        c.savecommit();
        cleanstage();
    }
    /** create a file with contents store in ContentShaID and put on path */
    public static void putFileTo(String filename,String ContentShaID, File path){
        File target = join(path,filename);
        String Content = readContentsAsString(join(blobs_DIR,ContentShaID));
        writein(Content,target,null);
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
    /** get the contents in the file
     * !!!! ONLY in String */
    public static String getItem(File file){
        return readContentsAsString(file);
    }

    public static String getShaID(File path, String filename) throws IOException {
        File f = Utils.join(path,filename);
        if(!f.exists()){
            System.out.println("File does not exist.");
            System.exit(0);
        }
        String items = getItem(f);
        return Utils.sha1(items);
    }
    /** return the ShaID of current commit's sha-1 ID(i.e. the parent of next commit) */
    public static String returnparentID(){
        String currenthead = Utils.readContentsAsString(HEAD);
        File headCommit = Utils.join(heads_DIR,currenthead);
        return Utils.readContentsAsString(headCommit);
    }
    /** check if the filename and the content shaID equivalent to the Parent commit */
    public static boolean IsinParentCommit(String filename, String shaID){
        String parentID = Repository.returnparentID(); // parent for next commit is current commit
        File parentfile = Utils.join(Repository.blobs_DIR, parentID);
        Commit parent = Utils.readObject(parentfile,Commit.class);
        //System.out.println(parent.Committree);
        if(IsSameFile(parent.Committree,filename,shaID)) // all same for filename and blobs
            return true;
        else return false;
        //TODO: need to fix when it appears to be two parents
        /*else if (!IsContent) // { !IsContent || !(IsFileName || IsContent) } either same filename but diff in contents or all diff
            return 1;
        else // diff in filename but same contents(blobs)
            return -1;*/
    }
    /** add the file to the current stage */
    public static void addtostage(String filename, String shaID) {//this shaID is the ID of blobs
        Staging Stage = Utils.readObject(Repository.INDEX, Staging.class);
        Stage.add(filename, shaID);
        boolean c = IsinParentCommit(filename, shaID);
        if (c) {
            System.out.println("already tracked in parent Commit");
            Stage.remove(filename);
        }
        // remove it from the staging area if it is already there (as can happen when a file is changed, added, and then changed back to it’s original version)
        // if c = 0 means filename and contents doesn't change in this version, so do not add
        writein(Stage, INDEX,null);// Storing stage
    }

    public static void removefile(String filename) throws IOException {
        Staging Stage = Utils.readObject(INDEX, Staging.class);
        TreeMap<String, String> add = Stage.getAddedFiles();
        File f = join(CWD, filename);
        String content = readContentsAsString(f);
        String sha1 =  sha1(content);
        boolean c = IsinParentCommit(filename,sha1);
        if(c){
            Stage.addToRemovedFiles(filename);//if track in parent commit, rm from CWD and stage for remove
            rmfilefrom(filename, CWD);
        }
        else if (add.containsKey(filename)) {//if it is stage for addidtion, unstage it
            add.remove(filename);
        }
        else{
            System.out.println("No reason to remove the file.");
            System.exit(0);}
        writein(Stage, INDEX,null);
    }
    /** delete file from CWD */
    public static void rmfilefrom(String filename,File path){
        File rmfile = join(path,filename);
        if(rmfile.exists()){
            restrictedDelete(rmfile);}
    }

    public static boolean checkisnull(String str){
        return str == null ;
    }

    /** overwrite or put the file tracked in the commit to the path */
    public static void overwritefile(String commitID, String filename, File path){
        Commit c = Commit.fromfile(commitID);
        //System.out.println(c.Committree);
        String fileshaID = c.Committree.get(filename);
        //System.out.println(fileshaID);
        File file = Utils.join(blobs_DIR,fileshaID);
        String content = readContentsAsString(file);
        //writein(content,path,filename);
        File targefile = join(path, filename);
        writeContents(targefile, content);
    }
    /** test if that filename exist in c*/
    public static boolean IsFilenameExist(String filename, Commit c){
        //System.out.println(c.Committree);
        return c.Committree.containsKey(filename);
    }

    public static void FileErrorTest(String filename, Commit c) throws IOException {
        if(!IsFilenameExist(filename,c)){
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
    }

    public static void CommitIDTest(String CommitID)  {
        File commit = join(blobs_DIR,CommitID);
        if(!commit.exists()){
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
    }

    public static void BranchTest(String branch) throws IOException {
        File branchfile = join(heads_DIR,branch);
        String head = readContentsAsString(HEAD);
        if(!branchfile.exists()){
            System.out.println("No such branch exists.");
            System.exit(0);
        }
        else if(Objects.equals(branch, head)){
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }
        else if(IsExistUntrackedFile()){
            System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
            System.exit(0);
        }
    }
    /** Is this file with the same content exist inside the tree*/
    public static boolean IsSameFile(TreeMap t, String filename, String ShaID){
        return t.containsKey(filename) && (t.get(filename).equals(ShaID)); // note can't use == to check equal
     }
     /** Is this a untracked file, if it is return true*/
    public static boolean IsUntrackedFile(String filename, String ShaID){
        Commit CurrentCommit = Commit.returncurrentCommit();
        boolean IsFilenameinCurrentCommit = IsFilenameExist(filename, CurrentCommit);// Is this file name exist in current commit
        Staging CurrentStage = readObject(INDEX, Staging.class);
        System.out.println(IsFilenameinCurrentCommit);
        TreeMap<String, String> addtree = CurrentStage.getAddedFiles();
        ArrayList<String> rmlist = CurrentStage.getRemovedFiles();
        boolean IsinAddtree = IsSameFile(addtree,filename,ShaID); // Is already add to stage
        boolean IsinDellist = rmlist.contains(filename); // Is already stage to rm
        System.out.println(IsinAddtree);
        System.out.println(IsinDellist);
        return !(IsFilenameinCurrentCommit || IsinAddtree || IsinDellist);
    }
    /** if there exist a untracked file in CWD return true, else return false */
    public static boolean IsExistUntrackedFile(){
        List<String> CWDFile = plainFilenamesIn(CWD);
        System.out.println(CWDFile);
        assert CWDFile != null;
        for(String filename : CWDFile){
            File file = join(CWD,filename);
            String ShaID = sha1(readContentsAsString(file));
            if(IsUntrackedFile(filename, ShaID)){
                return true;
            }
        }
        return false;
    }
    /** return the CommitID the branch point at */
    public static String getBranchID(String branchname){
        File branchfile = join(heads_DIR,branchname);
        String branchID = readContentsAsString(branchfile);
        return branchID;
    }
    /** three case for checkout cmd */
    public static void checkout(String commitID, String filename, String targetCommitID){
        if(checkisnull(targetCommitID)) {
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
            //File branchfile = join(heads_DIR,branch);
            //String targetshaID = readContentsAsString(targetcommitfile);
            Commit targetcommit = Commit.fromfile(targetCommitID);
            TreeMap<String, String> targetcommittree = targetcommit.Committree;
            Set<Map.Entry<String, String>> trackedFilePairs = targetcommittree.entrySet();// entry of branch committree
            Set<Map.Entry<String, String>> CurrentFilePairs = CurrentCommittree.entrySet();// entry of Current committree
            for(Map.Entry<String, String> filepair : CurrentFilePairs){
                String Currentfilename = filepair.getKey();
                String CurrentfileShaID = filepair.getValue();
                if(!IsSameFile(targetcommittree,Currentfilename,CurrentfileShaID)){
                    rmfilefrom(Currentfilename, CWD);}// del the file that is track in current commit but not in branch head
            }
            for(Map.Entry<String, String> trackedfilepair : trackedFilePairs){
                String trackedfilename = trackedfilepair.getKey();
                String trackedfileShaID = trackedfilepair.getValue();
                //System.out.println(trackedfilename);
                //System.out.println(trackedfileShaID);
                overwritefile(targetCommitID, trackedfilename, CWD);// put the file tracked in branch head commit to CWD
            }

        }
    }
    /** test if the branch in the head_dir exist */
    public static boolean IsBranchNameExist(String branchname){
        File branch = join(heads_DIR,branchname);
        if(branch.exists())
            return true;
        return false;
    }
    /** create a new branch and set the new branch point to the current head commit */
    public static void creatNewBranch(String branchname){
        File newBranch = join(heads_DIR,branchname);
        creatfile(newBranch);
        File CurrentHead = join(heads_DIR,getHEAD());
        String HeadCommitID = readContentsAsString(CurrentHead);
        writein(HeadCommitID, newBranch ,null);// set new branch at the current head commit
    }
    /** delete the branch */
    public static void delbranch(String branchname){
        File branch = join(heads_DIR,branchname);
        if(!branch.exists()){
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if(branchname.equals(getHEAD())){
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        rmfilefrom(branchname, heads_DIR);
    }
    public static ArrayList<String> getAllParentList(String startCommitID){
        ArrayList<String> ParentList = new ArrayList<>();
        getAllparent(startCommitID,ParentList);
        return ParentList;
    }
    /** Using DFS to travese through the commit tree and add those commit to the list*/
    private static void getAllparent(String StartCommitID, ArrayList<String> SavingList){
        Commit CurrentCommit = Commit.fromfile(StartCommitID);
        for(String parentID : CurrentCommit.getParentIDList()) {
            SavingList.add(parentID);
            Commit parent = Commit.fromfile(parentID);
            if(!parent.getParentIDList().isEmpty()){
                getAllparent(parentID, SavingList);
            }
        }
    }
    /** if the Candidate only has one than return that candidate,
     * else need to further compare which is the latest ancestor(count = 2)
     */
    public static String IsSplitpoint(int count, Queue<String> Candidates){
        System.out.println("this is count : " + count);
        String SplitPoint;
        if(count == 1) {
            SplitPoint = Candidates.poll();
            return SplitPoint;
        }
        else{
            String Candidate1, Candidate2;
            Candidate1 = Candidates.poll();
            Candidate2 = Candidates.poll();
            ArrayList<String> Candidate1ParentsList = getAllParentList(Candidate1);
            if(Candidate1ParentsList.contains(Candidate2)){
                System.out.println("this is Candidate 1 :" + Candidate1);
                return Candidate1;
            }
            else {
                System.out.println("this is Candidate 2 :" + Candidate2);
                return Candidate2;
            }
        }
    }

    /** Using BFS to find the SplitPoint comparing the all Given Branch's parents(ancestors) that is storing in the
     * List @param GivenBranchParentList.
     * The Current Head Start Commit ID is @param StartCommitID, Using BFS to match the latest common CommitID in the
     * parent List, if the node is merge from two common ancestor CommitID, than add to candiate and compare which is
     * latest one.
     * @return the Split point CommitID
     */
    public static String ReturnSplitPointID(String StartCommitID, List<String> GivenBranchParentList){
        Queue<String> queue = new LinkedList<>();
        queue.offer(StartCommitID);
        while(!queue.isEmpty()) {
            String CurrentCommitID = queue.poll();
            Commit CurrentCommit = Commit.fromfile(CurrentCommitID);
            int count = 0; // count the common ancestor of two merge branch in the parentID List
            Queue<String> Candidates = new LinkedList<>();
            for (String parentID : CurrentCommit.getParentIDList()) {
                Commit parentCommit = Commit.fromfile(parentID);
                if(GivenBranchParentList.contains(parentID)){
                    count += 1;
                    Candidates.add(parentID);// save the candidate to the queue
                }
                if (!parentCommit.getParentIDList().isEmpty()) {
                    queue.offer(parentID);
                }
            }
            if(count > 0){
                String Splitpoint = IsSplitpoint(count, Candidates);
                return Splitpoint;
            }
        }
        return null;
    }
    public static HashSet<String> Allfilesin3Commit(Commit Head, Commit BranchCommit, Commit SplitCommit){
        HashSet<String> AllfileSet = new HashSet<>();
        Set<Map.Entry<String,String>> HeadEntries = Head.Committree.entrySet();
        Set<Map.Entry<String,String>> BranchEntries = BranchCommit.Committree.entrySet();
        Set<Map.Entry<String,String>> SplitEntries = SplitCommit.Committree.entrySet();
        for(Map.Entry<String,String> entry : HeadEntries){
            AllfileSet.add(entry.getKey());
        }
        for(Map.Entry<String,String> entry : BranchEntries){
            AllfileSet.add(entry.getKey());
        }
        for(Map.Entry<String,String> entry : SplitEntries){
            AllfileSet.add(entry.getKey());
        }
        return AllfileSet;
    }
    public static boolean IsConflict(String filename, TreeMap<String,String> HeadTree, TreeMap<String,String> BranchTree, TreeMap<String,String> SplitTree){
        String SplitContent = SplitTree.get(filename);
        String HeadContent = HeadTree.get(filename);
        String BranchContent = BranchTree.get(filename);
        if(SplitTree.containsKey(filename)){
            if(HeadTree.containsKey(filename)){
                if(!SplitContent.equals(HeadContent)){
                    if(!HeadContent.equals(BranchContent) && !BranchContent.equals(SplitContent)) return true;
                    if(!BranchTree.containsKey(filename)) return true;
                }
            }
            else{ // else for Head not contain file
                if(!SplitContent.equals(BranchContent)) return true;
            }
        }
        else{
            if(HeadTree.containsKey(filename) && BranchTree.containsKey(filename)) {
                if (!HeadContent.equals(BranchContent)) return true;
            }
        }
        return false;
    }
    public static String rulesformerge(String filename, TreeMap<String,String> HeadTree, TreeMap<String,String> BranchTree, TreeMap<String,String> SplitTree){
        String SplitContent = SplitTree.get(filename);
        String HeadContent = HeadTree.get(filename);
        String BranchContent = BranchTree.get(filename);
        if(IsConflict(filename,HeadTree,BranchTree,SplitTree)){
            return "Conflict";
        }
        if(SplitTree.containsKey(filename)){
            if(HeadTree.containsKey(filename)){
                if(BranchTree.containsKey(filename)){
                    if(SplitContent.equals(HeadContent)) {
                        if (HeadContent.equals(BranchContent)) {
                            return "NotChange";
                        }
                        else {
                            return "Branch";
                        }
                    }
                    else {
                        if (SplitContent.equals(BranchContent)) {
                            return "NotChange";
                        }
                        else {
                            return "NotChange";
                        }
                    }
                }
                else{ // else for Branch Not contain file
                    return "Remove";
                }
            }
            else { // else for Head Not contain file
                if(BranchTree.containsKey(filename)){
                    return "NotChange";
                }
                else {
                    return "NotChange";
                }
            }
        }
        else{ // else for Split Not Contain file
            if (HeadTree.containsKey(filename)){
                return "NotChange";
            }
            else{
                return "Branch"; // add the branch content to the tree
            }
        }
    }
    public static void merge(String GivenBranch){
        if(GivenBranch.equals(getHEAD())){
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
        String HeadCommitID = returnparentID();
        Commit HeadCommit = Commit.fromfile(HeadCommitID);
        String BranchCommitID = readContentsAsString(join(heads_DIR,GivenBranch));
        Commit BranchCommit = Commit.fromfile(BranchCommitID);
        ArrayList<String> GivenBranchParentsList = getAllParentList(BranchCommitID);
        String SplitpointID = ReturnSplitPointID(HeadCommitID,GivenBranchParentsList);
        Commit SplitpointCommit = Commit.fromfile(SplitpointID);
        HashSet<String> Allfileset = Allfilesin3Commit(HeadCommit,BranchCommit,SplitpointCommit);
        Staging CurrentStage = readObject(INDEX,Staging.class);
        System.out.println(SplitpointID);
        for(String filename : Allfileset){
            String trigger = rulesformerge(filename,HeadCommit.Committree, BranchCommit.Committree, SplitpointCommit.Committree);
            System.out.println(filename);
            System.out.println(trigger);
            switch (trigger){
                case"NotChange":
                    break;
                case "Branch":
                    String BranchFileContent = BranchCommit.Committree.get(filename);
                    CurrentStage.add(filename, BranchFileContent);
                    break;
                case "Remove":
                    CurrentStage.addToRemovedFiles(filename);
                    break;
                case "Conflict":
                    System.out.println(filename);
                    String HeadContent;
                    String BranchContent;
                    String HeadContentshaID;
                    String BranchContentshaID;
                    if(!IsFilenameExist(filename, HeadCommit)){
                        BranchContentshaID = BranchCommit.Committree.get(filename);
                        HeadContent = "";
                        BranchContent = readContentsAsString(join(blobs_DIR,BranchContentshaID));
                    }
                    else if(!IsFilenameExist(filename,BranchCommit)){
                        HeadContentshaID = HeadCommit.Committree.get(filename);
                        BranchContent = "";
                        HeadContent = readContentsAsString(join(blobs_DIR,HeadContentshaID));
                    }
                    else {
                        HeadContentshaID = HeadCommit.Committree.get(filename);
                        BranchContentshaID = BranchCommit.Committree.get(filename);
                        HeadContent = readContentsAsString(join(blobs_DIR,HeadContentshaID));
                        BranchContent = readContentsAsString(join(blobs_DIR,BranchContentshaID));
                    }
                    System.out.println(PrintConflict(HeadContent,BranchContent));
                    System.exit(0);
            }

        }
        writein(CurrentStage, INDEX,null);
        String msg = String.format("Merged %s into %s.",GivenBranch,getHEAD());
        makecommit(msg,"Yi Shen",HeadCommitID,BranchCommitID);
    }
    public static void StageClearTest(){
        Staging CurrentStage = readObject(INDEX,Staging.class);
        TreeMap<String,String> Addtree = CurrentStage.getAddedFiles();
        ArrayList<String> Rmlist = CurrentStage.getRemovedFiles();
        if(!Addtree.isEmpty() || !Rmlist.isEmpty()){
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
    }
    public static String PrintConflict(String HeadContent, String BranchContent){
        return String.format("Encountered a merge conflict.\n\n"+"<<<<<<< HEAD\n" +
                "%s\n" +
                "=======\n" +
                "%s\n" +
                ">>>>>>>",HeadContent,BranchContent);
    }
}
