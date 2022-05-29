package gitlet;

// TODO: any imports you need here

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.TreeMap;

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Commit implements Serializable {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    private String message;
    private String parentID;
    private String author;
    private Date date;
    TreeMap<Object, Object> Committree;
    private static File branchfile;
    //static Staging Stagearea =new Staging();
    //private static TreeMap<Object, Object> Stage;

    String CommitID;

    /** Make and initial commit */
    public Commit(){
        this.message = "initial commit ";
        this.parentID = "";
        this.author = "Yi Shen";
        this.date = new Date(0);
        ArrayList<String> firstshalist = CommittoListString(this.message, this.author,this.date, this.parentID);
        this.CommitID = Utils.sha1(firstshalist);
    }
    public Commit(String msg, String parentshaID){
        Date dat = new Date();
        this.message = msg;
        this.parentID = parentshaID;
        this.author = "Yi Shen";
        this.date = dat;
        this.CommitID  = "";
        Committree = new TreeMap<>();
    }
    public Date getDate(){
        return this.date;
    }
    /** Set or change the HEAD with commitID
     * and create one if no such branch in heads_dir */
    public static void setHEAD(String commitID, String branch){
        branchfile = Utils.join(Repository.heads_DIR,branch);// dir that store branches
        Repository.writein(commitID, branchfile, null);
        Repository.writein(branch,Repository.HEAD,null );
        // if the heading commit already exist do not do write in (maybe in another helper method for going to certain commit)
    }
    /** initialize the commit with the specific msg and Date
     * also Create the Master branch and set HEAD to Master */
    public static void initialize(String aut){
        Commit first = new Commit();
        first.savecommit();
        setHEAD(first.CommitID, "master");
    }
    public void savecommit(){
        Repository.writein(this, Repository.blobs_DIR, this.CommitID);
    }

    public static Commit fromfile(String CommitID){
        Commit c;
        File commitfile = Utils.join(Repository.blobs_DIR, CommitID);
        c = Utils.readObject(commitfile, Commit.class);
        return c;
    }
    public static Commit returncurrentCommit(){
        String shaID = Repository.returnparentID();
        Commit c = fromfile(shaID);
        return c;
    }

    public static ArrayList<String> CommittoListString(String msg, String author, Date date, String parentID){
        ArrayList<String> stringlist =  new ArrayList<>();
        stringlist.add(msg);
        stringlist.add(author);
        stringlist.add(date.toString());
        stringlist.add(parentID);
        return stringlist;
    }
    /* TODO: fill in the rest of this class. */
}
