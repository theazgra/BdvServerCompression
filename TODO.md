## TODO list for compression library/application

Current LBG optimization algorithm for VQ is not optimal and we should try more algorithms:
- [Equitz algorithm](https://labs.oracle.com/pls/apex/f?p=LABS:0::APPLICATION_PROCESS%3DGETDOC_INLINE:::DOC_ID:370)
  - [k-d tree](https://www.cs.cmu.edu/~ckingsf/bioinfo-lectures/kdtrees.pdf)
- [Maximum Descent](https://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=577022)
  - *"In image vector quantization, the MD algorithm can produce codebooks with about 1 dB improvement in peak signal to noise ratio in less than MOO of the time required by the LBG algorithm"*
- [BA-LBG which uses Bat Algorithm](https://www.sciencedirect.com/science/article/pii/S2215098615001664)
  - *"BA-LBG uses Bat Algorithm on initial solution of LBG. It produces an efficient codebook with less computational time and results very good PSNR due to its automatic zooming feature using adjustable pulse emission rate and loudness of bats."*
  - *"Its average convergence speed is 1.841 times faster than FA-LBG."*