FROM ocaml/opam:ubuntu-lts-ocaml-4.14

USER root
RUN apt-get update -y && \
    apt-get install -y software-properties-common gcc-multilib make m4 && \
    rm -rf /var/lib/apt/lists/*

USER opam
RUN opam init --disable-sandboxing -y
RUN opam pin add -y ostap 0.6
RUN opam pin add -y dune 3.7.1
RUN opam pin add -y bisect_ppx 2.8.3
RUN opam pin add -y posix-uname 2.0.1
RUN git clone https://github.com/e2e4b6b7/Lama.git --depth 1 --branch 1.30-coverage
WORKDIR /home/opam/Lama
RUN eval $(opam env); dune build --instrument-with bisect_ppx

CMD ["sleep", "infinity"]
