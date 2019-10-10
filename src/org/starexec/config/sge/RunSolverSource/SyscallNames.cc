#include "SyscallNames.hh"

const char *syscallNames[nbSyscallNames]={
	"???"
};

const char *getSyscallName(int n)
{
  if (n>0 && n<=nbSyscallNames)
    return syscallNames[n];
  else
    return "???";
}
