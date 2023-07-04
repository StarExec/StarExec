@Author Alex Guo of UIowa

The purpose of this directory is to have a test pipeline such that we can 
create test jobs for pipelines. Please see the source file for more information.

I have included test.xml, which you need to submit to the server. If you don't use the
provided XML file, you will not get any stages. You also need to replace the bench-id
and config-id with the ids of the config and benchmark you will be using. This information
can be aquired by running a job with your desired config/benchmark, downloading the job XML, 
and copying the values. If you run into any unanticipated errors, more documentation can be found here: https://wiki.uiowa.edu/display/stardev/User+Guide.

I have also included the source code in C, as well as the benchmark and the text file it was 
creared from.

The dummySolver.tar contains the executable and config files. Starexec should automagically find 
the config and add it to the solver. The solver was compiled using GCC for redhat linux. 


Here is sample output for each stage
------------------------------------------------------------------------------------------
Stage 1:
0.00/0.01	Starexec4ever!
0.00/0.01	Starexec4ever! 
0.00/0.01	EOF
------------------------------------------------------------------------------------------
Stage 2: 
0.00/0.01	0.00/0.01	Starexec4ever!
0.00/0.01	0.00/0.01	Starexec4ever! 
0.00/0.01	0.00/0.01	EOF
0.00/0.01	
0.00/0.01	Starexec4ever! 
0.00/0.01	EOF
------------------------------------------------------------------------------------------
Stage 3: 
0.00/0.00	0.00/0.01	0.00/0.01	Starexec4ever!
0.00/0.00	0.00/0.01	0.00/0.01	Starexec4ever! 
0.00/0.00	0.00/0.01	0.00/0.01	EOF
0.00/0.00	0.00/0.01	
0.00/0.00	0.00/0.01	Starexec4ever! 
0.00/0.00	0.00/0.01	EOF
0.00/0.00	
0.00/0.00	Starexec4ever! 
0.00/0.01	EOF





The MIT License (MIT)

Copyright (c) 2012-18 The University of Iowa

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
