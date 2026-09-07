package com.hsc.api.service;


import com.hsc.common.exception.ParserException;

public interface IFsXmlCdrService {
    void cdrHandler(String reqText) throws ParserException;
}
