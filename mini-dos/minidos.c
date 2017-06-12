#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <errno.h>
#include <time.h>

#define BLOCK_SIZE     512   
#define NAME_LEN_LIMIT  16  // length limit for name
#define DIRC_LIMIT      16  // max # of items in a directory 
#define USER_LIMIT      15  // max # of users
#define FAT_SIZE       124  //  

#define SUPERBLOCK 0

// permision bits: 3 for owner, 3 for others
#define S_IROWN  1
#define S_IWOWN  2
#define S_IXOWN  4
#define S_IROTH  8
#define S_IWOTH 16
#define S_IXOTH 32

typedef uint32_t block_ptr_t;

// meta data structure of file/directory
typedef struct {
  uint8_t  owner; // owner's user id
  uint8_t  perm;  // permission as bit field
  uint32_t ctime; // created time
  uint32_t mtime; // last modified time
} meta;

typedef struct {
  char username[NAME_LEN_LIMIT];
  char password[NAME_LEN_LIMIT];
} userinfo; 

// directory entry types
typedef enum {
  DIRC_FILE,    // a file
  DIRC_DIR,     // a directory
} direty;

// directory entry information
typedef struct {
  direty ty;                  // entry type file/directory?
  block_ptr_t b;              // block containg the actual file/directory object
  char name[NAME_LEN_LIMIT];  // name of the file/directory
} dircinfo; 

typedef union {
  // a raw block
  uint8_t bytes[BLOCK_SIZE];

  // structure of super block
  struct {
    uint32_t bc;       // block count
    block_ptr_t free;  // first free block #
    block_ptr_t root;  // root directory block #

    uint8_t uc;                 // user count
    userinfo users[USER_LIMIT]; // user info for each user
  } super;
  
  // structure of a free block
  struct {
    block_ptr_t next;  // next block #
  } free;

  // structure of a directory block
  struct {
    meta m;                      // directory meta data
    block_ptr_t parent;          // parent directory
    uint8_t fc;                  // child count
    dircinfo dirc[DIRC_LIMIT];   // child items file or sub directory
  } dir;

  // structure of a file block
  struct {
    meta m;                      // file meta data
    uint32_t size;               // file size
    block_ptr_t fat[FAT_SIZE];   // file allocation table
  } file;

} block;

void create(const char *disk_file_path);
void execute();
void login();
void prompt();

void cd       (const char* param);
void ls       ();
void diskinfo ();
void cat      (const char* param);
void rm       (const char* param);
void rmdir    (const char* param);
void mkdir    (const char* param);
void cp       (const char* param);
void mv       (const char* param);
void chmod    (const char* param);
void chown    (const char* param);
void ed       ();
void unknown  (const char* cmd);

block block_get(uint32_t i);
void  block_put(uint32_t i, block b); 
void  error_exit(uint32_t i, const char *fnc);

void sample_dir_entry(block *rb, int index, int blockno, direty ty, const char *name);
block get_sample_file_block(int size);
block get_sample_dir_block();

char user[80];
char *diskimage;
char *cwdpath = "/";

FILE *disk = NULL;
int  cwd = 1; // root or block #1

int main(int argc, char** argv)
{
  // a quick sanity check
  if (sizeof(block) != BLOCK_SIZE) {
    printf("Fatal error: sizeof(block) = %lu, BLOCK_SIZE = %d\n", sizeof(block), BLOCK_SIZE);
    exit(1);
  }

  if (argc != 2) {
    printf("Useage: mcdos <disk>\n");
    exit(1);
  }

  diskimage = argv[1];
  disk = fopen(diskimage, "r+");

  if (disk)
    execute();
  else
    create(argv[1]);

  return 0;
}

void create(const char *disk_file_path)
{
  int size, uc, i;
  char s[80];

  block sb; // super block
  block rb; // root dir block
  block fb; // free block

  printf("Enter disk size (bytes): "); scanf("%d", &size);
  printf("Enter user count: ");        scanf("%d", &uc);  
  sb.super.uc = uc;

  for(i=0; i<uc; i++) {
    printf("User %d:\n", i+1);

    printf("  Enter name: "); 
    scanf("%s", s);
    strncpy(sb.super.users[i].username, s, NAME_LEN_LIMIT);    

    printf("  Enter password: "); 
    scanf("%s", s);
    strncpy(sb.super.users[i].password, s, NAME_LEN_LIMIT);
  }

  printf("Creating disk on: %s\n", disk_file_path);

  // create a disk file
  disk = fopen(disk_file_path, "w");  

  // calculare block count. 
  int bc = (size/BLOCK_SIZE) + (size % BLOCK_SIZE > 0 ? 1 : 0);  

  // create some sample files & directory 
  rb.dir.parent = 0;
  rb.dir.fc = 3;     
  sample_dir_entry(&rb, 0, 2, DIRC_FILE, "file1");
  sample_dir_entry(&rb, 1, 3, DIRC_FILE, "file2");
  sample_dir_entry(&rb, 2, 4, DIRC_DIR,  "dir1");

  sb.super.bc = bc;  
  sb.super.root = 1;
  sb.super.free = rb.dir.fc + 2; 

  block_put(0, sb);
  block_put(1, rb);
  block_put(2, get_sample_file_block(123));
  block_put(3, get_sample_file_block(321));
  block_put(4, get_sample_dir_block());

  for (i=5; i<bc-1; i++) {
    fb.free.next = i+1; 
    block_put(i, fb);  
  }
  fb.free.next = 0; 
  block_put(i, fb);  

  fclose(disk);
  printf("Done!\n");
}


void execute()
{
  char line[80];
  size_t size;
  char *cmd, *param;

  login();

  while (1) {
    prompt();
    scanf("%s", line);
    cmd = strtok(line, " \t\n");
    param = strtok(NULL, "\n");

         if (strcmp(cmd, "cd")       == 0) cd      (param);
    else if (strcmp(cmd, "ls")       == 0) ls      ();
    else if (strcmp(cmd, "diskinfo") == 0) diskinfo();
    else if (strcmp(cmd, "cat")      == 0) cat     (param);
    else if (strcmp(cmd, "rm")       == 0) rm      (param);
    else if (strcmp(cmd, "rmdir")    == 0) rmdir   (param);
    else if (strcmp(cmd, "mkdir")    == 0) mkdir   (param);
    else if (strcmp(cmd, "cp")       == 0) cp      (param);
    else if (strcmp(cmd, "mv")       == 0) mv      (param);
    else if (strcmp(cmd, "chmod")    == 0) chmod   (param);
    else if (strcmp(cmd, "chown")    == 0) chown   (param);
    else if (strcmp(cmd, "exit")     == 0) break;
    else if (strcmp(cmd, "ed")       == 0) ed      ();
    else                                   unknown (cmd);
  }

  fclose(disk);
}

void login()
{
  block b;
  char u[80];
  char p[80];
  int i;
  int valid = 0;

  b = block_get(SUPERBLOCK);
  
  while (1) {
    printf("%s login: ", diskimage);
    scanf("%s", u);
    printf("password: ");
    scanf("%s", p);       

    for (i=0; i<b.super.uc; i++) {
      if (strcmp(u, b.super.users[i].username) == 0 && 
          strcmp(p, b.super.users[i].password) == 0) {
        strcpy(user, u);
        valid = 1;
        break;
      }
    }
    
    if (!valid) 
      printf("Invalid login or password!\n");
    else
      break;
  }
}

void prompt()
{
  printf("%s@%s:%s$ ", user, diskimage, cwdpath);
}

void cd(const char* param)
{

}

void ls()
{
  int i;
  meta m;
  dircinfo dc;
  block b; 
  block sb = block_get(SUPERBLOCK); // super block
  block db = block_get(cwd); // current working dir block
  
  printf("total %d\n", db.dir.fc);
  for (i = 0; i < db.dir.fc; i++) {
    dc = db.dir.dirc[i];
    b = block_get(dc.b);
    m = b.dir.m;

    printf("%c%c%c%c%c%c%c %4d %s %4d %d %d %s\n",
      (dc.ty == DIRC_FILE ? '-' : 'd'),  /* directory or file */
      (m.perm & S_IROWN ? 'r' : '-'),    /* can owner read? */
      (m.perm & S_IWOWN ? 'w' : '-'),    /* can owner write? */
      (m.perm & S_IXOWN ? 'x' : '-'),    /* can owner execute? */
      (m.perm & S_IROTH ? 'r' : '-'),    /* can others read? */
      (m.perm & S_IWOTH ? 'w' : '-'),    /* can others write? */
      (m.perm & S_IXOTH ? 'x' : '-'),    /* can others execute? */
      dc.b,                              /* the block number */
      sb.super.users[m.owner].username,  /* owner's name */
      (dc.ty == DIRC_FILE ? b.file.size : BLOCK_SIZE), /* file size */
      m.ctime,   /* created */
      m.mtime,   /* last modified */
      dc.name);  /* name of file or directory */
  }
}

void diskinfo()
{
  block    b    = block_get(SUPERBLOCK);
  uint32_t bc   = b.super.bc;
  uint32_t free = 0;
  uint32_t i    = b.super.free; 

  while (i != 0) {
    b = block_get(i);
    free++;
    i = b.free.next;
  }

  printf("Total blocks: %d\n", bc);
  printf(" Free blocks: %d\n", free);
  printf(" Used blocks: %d\n", bc - free); 
}

void cat(const char* param)
{

}

void rm(const char* param)
{

}

void rmdir(const char* param)
{

}

void mkdir(const char* param)
{

}

void cp(const char* param)
{

}

void mv(const char* param)
{

}

void chmod(const char* param)
{

}

void chown(const char* param)
{

}

void ed()
{

}

void unknown(const char* cmd)
{  
  printf("The program '%s' is currently not installed.\n", cmd);
}

block block_get(uint32_t i) 
{
  block b;
  
  if (fseek(disk, i * sizeof(block), SEEK_SET) == -1) 
    error_exit(i, "fseek");
  
  if (fread(&b, sizeof(block), 1, disk) != 1) 
    error_exit(i, "fread");
  
  return b;
}

void block_put(uint32_t i, block b)
{
  if (fseek(disk, i * sizeof(block), SEEK_SET) == -1) 
    error_exit(i, "fseek");

  if (fwrite(&b, sizeof(block), 1, disk) != 1) 
    error_exit(i, "fwrite");
}

void error_exit(uint32_t i, const char *fnc)
{
  printf("%s failed. block:%d errno:%d\n", fnc, i, errno);
  exit(1);
}

void sample_dir_entry(block *rb, int index, int blockno, direty ty, const char *name)
{
  rb->dir.dirc[index].b = blockno;
  rb->dir.dirc[index].ty = ty;
  strcpy(rb->dir.dirc[index].name, name);  
}

block get_sample_file_block(int size)
{
  block b;

  b.file.m.owner = 0;
  b.file.m.perm = S_IROWN + S_IWOWN + S_IXOWN + S_IROTH + S_IXOTH;
  b.file.m.ctime = time(NULL);
  b.file.m.mtime = time(NULL);
  b.file.size = size;

  return b;
}

block get_sample_dir_block()
{
  block b;

  b.dir.m.owner = 0;
  b.dir.m.perm = S_IROWN + S_IWOWN + S_IXOWN + S_IROTH + S_IXOTH;
  b.dir.m.ctime = time(NULL);
  b.dir.m.mtime = time(NULL);
  b.dir.parent = 0;
  b.dir.fc = 0;

  return b;
}

