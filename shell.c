#include <stdio.h>
#include <string.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <sys/types.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <sys/wait.h>

#define I 0//input
#define O 1//output
#define A 2//append

void removeSpaces(char* buf){
	if(buf[strlen(buf)-1]==' ' || buf[strlen(buf)-1]=='\n')
	buf[strlen(buf)-1]='\0';
	if(buf[0]==' ' || buf[0]=='\n') memmove(buf, buf+1, strlen(buf));
}

void bufferTokken(char** param,int *nr,char *buf,const char *c){
	char *t;
	t=strtok(buf,c);
	int pc=-1;
	while(t){
		param[++pc]=malloc(sizeof(t)+1);
		strcpy(param[pc],t);
		removeSpaces(param[pc]);
		t=strtok(NULL,c);
	}
	param[++pc]=NULL;
	*nr=pc;
}

void commandParameters(char ** param){
	while(*param){
		printf("param=%s..\n",*param++);
	}
}

void basicCommand(char** argv){
	if(fork()>0){
		
		wait(NULL);
	}
	else{
		
		execvp(argv[0],argv);
		
		perror( "invalid command ");
		exit(1);
	}
}

void pipedCommand(char** buf,int nr){
	if(nr>10) return;

    int fd[10][2];
    int i;
    int pc;
    char *argv[100];

	for(i=0;i<nr;i++){
		bufferTokken(argv,&pc,buf[i]," ");
		if(i!=nr-1){
			if(pipe(fd[i])<0){
				perror("piping error!\n");
				return;
			}
		}
		if(fork()==0){//child1
			if(i!=nr-1){
				dup2(fd[i][1],1);
				close(fd[i][0]);
				close(fd[i][1]);
			}

			if(i!=0){
				dup2(fd[i-1][0],0);
				close(fd[i-1][1]);
				close(fd[i-1][0]);
			}
			execvp(argv[0],argv);
			perror("invalid command ");
			exit(1);
		}
		
		if(i!=0){
			close(fd[i-1][0]);
			close(fd[i-1][1]);
		}
		wait(NULL);
	}
}

void assyncCommand(char** buf,int nr){
	int i,pc;
	char *argv[100];
	for(i=0;i<nr;i++){
		bufferTokken(argv,&pc,buf[i]," ");
		if(fork()==0){
			execvp(argv[0],argv);
			perror("invalid command ");
			exit(1);
		}
	}
	for(i=0;i<nr;i++){
		wait(NULL);
	}

}

void redirectCommand(char** buf,int nr,int mode){
	int pc,fd;
	char *argv[100];
	removeSpaces(buf[1]);
	bufferTokken(argv,&pc,buf[0]," ");
	if(fork()==0){

		switch(mode){
		case I:  fd=open(buf[1],O_RDONLY); break;
		case O: fd=open(buf[1],O_WRONLY); break;
		case A: fd=open(buf[1],O_WRONLY | O_APPEND); break;
		default: return;
		}

		if(fd<0){
			perror("cannot open file\n");
			return;
		}

		switch(mode){
		case I:  		dup2(fd,0); break;
		case O: 		dup2(fd,1); break;
		case A: 		dup2(fd,1); break;
		default: return;
		}
		execvp(argv[0],argv);
		perror("invalid I ");
		exit(1);
	}
	wait(NULL);
}


int main(char** argv,int argc)
{
	char buf[500],*buffer[100],buf2[500],buf3[500], *command[100],*params2[100],*t,cwd[1024];
	int nr=0;
	while(1){
		printf("$>");
        fgets(buf, 500, stdin);

		
		if(strchr(buf,'|')){
			bufferTokken(buffer,&nr,buf,"|");
			pipedCommand(buffer,nr);
		}
		else if(strchr(buf,'&')){
			bufferTokken(buffer,&nr,buf,"&");
			assyncCommand(buffer,nr);
		}
		else if(strstr(buf,">>")){
			bufferTokken(buffer,&nr,buf,">>");
			if(nr==2)redirectCommand(buffer,nr,A);
			else printf("Command Error!)");
		}
		else if(strchr(buf,'>')){
			bufferTokken(buffer,&nr,buf,">");
			if(nr==2)redirectCommand(buffer,nr, O);
			else printf("Command Error!");
		}
		else if(strchr(buf,'<')){
			bufferTokken(buffer,&nr,buf,"<");
			if(nr==2)redirectCommand(buffer,nr, I);
			else printf("Command Error!");
		}
		else{
			bufferTokken(command,&nr,buf," ");
			if(strstr(command[0],"cd")){
				chdir(command[1]);
			}
			
			else if(strstr(command[0],"version")){
				printf("Author: Kenny\nVersion: 1.0.0\n\n");
			}
			else if(strstr(command[0],"exit")){
				exit(0);
			}
            else if(strstr(command[0],"help")){
				printf("Supported Command\n");
                printf("cd  with/without argument\n");
                printf("mv  with/without argument\n");
				printf("dirname command\n");
                printf("Piping commnand e,g commmand 1 | command 2 | command 3 \n");
                printf("Redirection e,g command > file or >> file or < file 1\n");
                printf("Piped+ redirection eg, command  1 | command 2  > file 1\n");
                printf("All Other buildin commands....\n");
			}
			else basicCommand(command);
		}
	}

	return 0;
}