#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/*
This is a test solver written by Alex Guo.
The purpose of this solver is to test the fact that 
the pipeline actually works. This solver prints to 
std out the contents of a file, followed by the 
string "Starexec4ever! \n"
*/


int main(int argc, char** argv) {

    if (argc != 2) {
        printf("you must have one argument\n");
        exit(1);
    }

    //here is the file pointer
    FILE* file = fopen(argv[1], "r");
    char ch;

 
    if (NULL == file) {
        printf("file can't be opened \n");
        exit(1);
    }

    char buff[50];
    int size = 0;
    // Printing what is written in file
    // character by character using loop.
    while ((ch = fgetc(file)) != EOF) {
        //please do not attempt buffer overflow on our machines ;)
        if (size < 49) {
        buff[size] = ch;
        size++;
        }
    }
    buff[size] = '\0';
    printf("%s\n", buff);
    printf("Starexec4ever! \n");

    fclose(file);
    return 0;


}