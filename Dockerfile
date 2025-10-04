FROM amazoncorretto:21-alpine
CMD mkdir src
COPY frontend/app/build/dist/js/productionExecutable src

COPY backend/build/distributions/backend.tar backend.tar
RUN tar -xf backend.tar
RUN rm backend.tar
RUN mkdir logs

ENTRYPOINT ["./backend/bin/backend"]
