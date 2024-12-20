# Fuzzer of Lama programming language

## How to start

To start just run `./strart.sh` (At least on Linux).
It will create (or re-use) container with Lama compiler
and run the fuzzer itself.
Any info about generations and found bugs will be printed into stdout.
Any info about expected errors will be written into files in directory `reports`

## How it works

Simple evolutionary fuzzer. 
Inintial population initialized as random expressions with depth <= 3.
Then each survived expression mutated by replacing random node with new expression with depth <= 3.

## Results

It covers some amount of lama compiler.
It is hard to say how many sound lines left as coverage includes all files even stack-machine level interpreter, 32-bit compiler, etc.

It found two different crashes of the compiler (saved in `crashes` directory)

## What is inside

- `EvolutionaryFuzzer`.
  Runs evolution, selection and mutation. Reports new crashes.
- `PredefinedReporters`
  Filters out errors that are known to may happen:
  - Syntax error. Happens as I did not covered all legacy keywords, etc.
  - Already defined error. Happens as mutations did not have full context of the mutated hole.
  - Undefined error. Happens as mutations of patterns may remove some bindings which are used in branch body.
  - Indirect assignment error. This bug is not easy to fix, so it is currently just an expected error of the compiler.
  These errors are not a problem, as they are happens quite rarely and immediately filtered out of population.
- `mutators/RandGenerator`
  Randomly generates any type of node in ast up to some limited depth.
- `mutators/SimpleMutator`
  Replaces random node with randomly generated node up to depth 3.
- `executors/DockerExecutor`
  Communicates with existing docker container.
  Runs compilation of lama files and collects coverage.
- `ast/AST`
  AST of Lama language
- `ast/Scope`
  Scope of the names available in the program.
- `ast/Transformer`
  Very imperformant and simple transformer of AST.
- `ast/Printer`
  Very simple printer of AST into Lama code by prining it in one line.
- `ast/CountingTransformer`
  Special transformer to calculate amount of mutation point to randomly choose one later.

## What should be done next

- Parsing of initial seeds from `seeds` directory
- Parallelization of lama compilations
- Analysis if main file X86_64 fully covered
- Addition of checks that output of interpreters are equal to output of the execution
