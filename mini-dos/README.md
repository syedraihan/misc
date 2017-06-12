Approach
========
* The disk is divided into fixed sized blocks

* A block can be of 5 types:
  1. super block - contains pointers to root and head of free block chain
     as well as user list
  2. diretory info block
  3. file info block
  4. free block
  5. a block that contains file content.

* The block 0 will be a super block which contains 
  pointers to root directory and head of free block chain
  as well as user list

* A directory info block will contain a link to its parent, 
  meta data like permission, created/last modified date, owner and 
  and a list of all child (file/directory). 
  Each record for child item will contain it's type (file/directory), name
  which and the block number where it's information can be found.

* A file info block will contain it's size, meta data and
  most importantly the File Allocation Table, 

* A free block simply has pointer to next free block.
  If the next pointer is zero, it means the linked list ends.

* Any other block is a block that contains contant of a file.

The folowing features is implemented:
------------------
1. The create mode. It asks for disk size and user lists and then
   creates and formats the disk apropriately.
   It also adds some sample files/folder for testing the ls command.

2. The login process. In execution mode, it asks for login/pass and 
   vlidates against the information stored in the super block.

3. The execute mode. It shows a linux like prompt and 
   It takes a command from user and invokes apropriate function.
   It can handle unknown command gracefully.
   It does command parsing also.

4. diskinfo command has been implemented.

5. ls command has been implemented.

6. exit command has been implemented.



