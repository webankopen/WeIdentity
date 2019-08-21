/*
 *       Copyright© (2018-2019) WeBank Co., Ltd.
 *
 *       This file is part of weid-java-sdk.
 *
 *       weid-java-sdk is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU Lesser General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or
 *       (at your option) any later version.
 *
 *       weid-java-sdk is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU Lesser General Public License for more details.
 *
 *       You should have received a copy of the GNU Lesser General Public License
 *       along with weid-java-sdk.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.webank.weid.full.evidence;

import java.util.Map;

import mockit.Mock;
import mockit.MockUp;
import org.bcos.web3j.crypto.Sign.SignatureData;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.webank.weid.util.CredentialUtils.copyCredential;

import com.webank.weid.constant.ErrorCode;
import com.webank.weid.constant.ParamKeyConstant;
import com.webank.weid.full.TestBaseServcie;
import com.webank.weid.protocol.base.Credential;
import com.webank.weid.protocol.base.CredentialPojo;
import com.webank.weid.protocol.base.CredentialWrapper;
import com.webank.weid.protocol.response.CreateWeIdDataResult;
import com.webank.weid.protocol.response.ResponseData;
import com.webank.weid.service.impl.EvidenceServiceImpl;
import com.webank.weid.util.DateUtils;

/**
 * TestVerifyEvidence v_wbpenghu.
 */
public class TestVerifyEvidence extends TestBaseServcie {

    private static final Logger logger = LoggerFactory.getLogger(TestVerifyEvidence.class);
    private static Credential evidenceCredential = null;
    private static String evidenceAddress;

    @Override
    public synchronized void testInit() {
        if (!isInitIssuer) {
            super.testInit();
        }
        if (evidenceCredential == null) {
            evidenceCredential = super.createCredential(createCredentialArgs).getCredential();
            String hashValue = credentialService.getCredentialHash(evidenceCredential).getResult();
            ResponseData<String> evidence = evidenceService.createEvidence(hashValue,
                createWeIdResultWithSetAttr.getUserWeIdPrivateKey());
            Assert.assertTrue(!evidence.getResult().isEmpty());
            evidenceAddress = evidence.getResult();
        }
    }

    /**
     * case1: succeed.
     */
    @Test
    public void testVerifyEvidenceCase1() {
        String hashValue = credentialService.getCredentialHash(evidenceCredential).getResult();
        ResponseData<Boolean> responseData = evidenceService.verify(hashValue, evidenceAddress);
        logger.info("testVerifyEvidenceCase1 result :" + responseData);
        Assert.assertTrue(responseData.getResult());
        Assert.assertEquals(responseData.getErrorCode().intValue(), ErrorCode.SUCCESS.getCode());
    }

    /**
     * case2: id is "".
     */
    @Test
    public void testVerifyEvidenceCase2() {
        Credential credential = copyCredential(evidenceCredential);
        credential.setId("");
        ResponseData<String> innerResp = credentialService.getCredentialHash(credential);
        String hashValue = innerResp.getResult();
        Assert.assertEquals(
            ErrorCode.CREDENTIAL_ID_NOT_EXISTS.getCode(),
            innerResp.getErrorCode().intValue());
        ResponseData<Boolean> responseData = evidenceService
            .verify(hashValue, evidenceAddress);
        logger.info("testVerifyEvidenceCase2 result :" + responseData);
        Assert.assertFalse(responseData.getResult());
    }

    /**
     * case3: id is null.
     */
    @Test
    public void testVerifyEvidenceCase3() {
        Credential credential = copyCredential(evidenceCredential);
        credential.setId(null);
        ResponseData<String> innerResp = credentialService.getCredentialHash(credential);
        String hashValue = innerResp.getResult();
        Assert.assertEquals(
            ErrorCode.CREDENTIAL_ID_NOT_EXISTS.getCode(),
            innerResp.getErrorCode().intValue());
        ResponseData<Boolean> responseData = evidenceService
            .verify(hashValue, evidenceAddress);
        logger.info("testVerifyEvidenceCase3 result :" + responseData);
        Assert.assertFalse(responseData.getResult());
    }


    /**
     * case5: Issuer is "".
     */
    @Test
    public void testVerifyEvidenceCase5() {
        Credential credential = copyCredential(evidenceCredential);
        credential.setId(credential.getId());
        credential.setIssuer("");
        ResponseData<String> innerResp = credentialService.getCredentialHash(credential);
        String hashValue = innerResp.getResult();
        Assert.assertEquals(
            ErrorCode.CREDENTIAL_ISSUER_INVALID.getCode(),
            innerResp.getErrorCode().intValue());
        ResponseData<Boolean> responseData = evidenceService
            .verify(hashValue, evidenceAddress);
        logger.info("testVerifyEvidenceCase5 result :" + responseData);
        Assert.assertFalse(responseData.getResult());
    }

    /**
     * case6: cptId is not exit.
     */
    @Test
    public void testVerifyEvidenceCase6() {
        Credential credential = copyCredential(evidenceCredential);
        credential.setId(credential.getId());
        credential.setCptId(-1);
        ResponseData<String> innerResp = credentialService.getCredentialHash(credential);
        String hashValue = innerResp.getResult();
        Assert.assertEquals(
            ErrorCode.CPT_ID_ILLEGAL.getCode(),
            innerResp.getErrorCode().intValue());
        ResponseData<Boolean> responseData = evidenceService
            .verify(hashValue, evidenceAddress);
        logger.info("testVerifyEvidenceCase6 result :" + responseData);
        Assert.assertFalse(responseData.getResult());
    }

    /**
     * case7: ExpirationDate is not match.
     */
    @Test
    public void testVerifyEvidenceCase7() {
        Credential credential = copyCredential(evidenceCredential);
        credential.setExpirationDate(System.currentTimeMillis() - 5000);
        ResponseData<String> innerResp = credentialService.getCredentialHash(credential);
        String hashValue = innerResp.getResult();
        Assert.assertTrue(innerResp.getErrorCode() == ErrorCode.CREDENTIAL_EXPIRED.getCode()
            || innerResp.getErrorCode() == ErrorCode.CREDENTIAL_EVIDENCE_HASH_MISMATCH.getCode());
        ResponseData<Boolean> responseData = evidenceService
            .verify(hashValue, evidenceAddress);
        logger.info("testVerifyEvidenceCase7 result :" + responseData);
        Assert.assertFalse(responseData.getResult());

    }

    /**
     * case8: IssuranceDate is not match.
     */
    @Test
    public void testVerifyEvidenceCase8() {
        Credential credential = copyCredential(evidenceCredential);
        credential.setIssuanceDate(DateUtils.getNoMillisecondTimeStamp());
        String hashValue = credentialService.getCredentialHash(credential).getResult();
        ResponseData<Boolean> responseData = evidenceService
            .verify(hashValue, evidenceAddress);
        logger.info("testVerifyEvidenceCase8 result :" + responseData);
        Assert.assertFalse(responseData.getResult());
        Assert.assertEquals(responseData.getErrorCode().intValue(),
            ErrorCode.CREDENTIAL_EVIDENCE_HASH_MISMATCH.getCode());
    }

    /**
     * case10: args is null.
     */
    @Test
    public void testVerifyEvidenceCase10() {
        String hashValue = credentialService.getCredentialHash(evidenceCredential).getResult();
        ResponseData<Boolean> responseData = evidenceService
            .verify(hashValue, null);
        logger.info("testVerifyEvidenceCase12 result :" + responseData);
        Assert.assertFalse(responseData.getResult());
        Assert.assertEquals(responseData.getErrorCode().intValue(),
            ErrorCode.ILLEGAL_INPUT.getCode());
    }

    /**
     * case11: Signature is not match.
     */
    @Test
    public void testVerifyEvidenceCase11() {
        Credential credential = copyCredential(evidenceCredential);
        Map<String, String> proof = credential.getProof();
        String sigValue = proof.get(ParamKeyConstant.CREDENTIAL_SIGNATURE);
        proof.put(ParamKeyConstant.CREDENTIAL_SIGNATURE, sigValue + "x");
        credential.setProof(proof);
        String hashValue = credentialService.getCredentialHash(credential).getResult();
        ResponseData<Boolean> responseData = evidenceService
            .verify(hashValue, evidenceAddress);
        logger.info("testVerifyEvidenceCase11 result :" + responseData);
        Assert.assertFalse(responseData.getResult());
        Assert.assertEquals(responseData.getErrorCode().intValue(),
            ErrorCode.CREDENTIAL_EVIDENCE_HASH_MISMATCH.getCode());
    }

    /**
     * case12: args is null.
     */
    @Test
    public void testVerifyEvidenceCase12() {
        String hashValue = null;
        ResponseData<Boolean> responseData = evidenceService.verify(hashValue, evidenceAddress);
        logger.info("testVerifyEvidenceCase12 result :" + responseData);
        Assert.assertFalse(responseData.getResult());
        Assert.assertEquals(responseData.getErrorCode().intValue(),
            ErrorCode.ILLEGAL_INPUT.getCode());
    }

    /**
     * case13: privateKey is not match.
     */
    @Test
    public void testVerifyEvidenceCase13() {
        CreateWeIdDataResult weIdWithSetAttr = super.copyCreateWeId(createWeIdResultWithSetAttr);
        weIdWithSetAttr.getUserWeIdPrivateKey().setPrivateKey("11111111");
        String hashValue = credentialService.getCredentialHash(evidenceCredential).getResult();
        ResponseData<String> responseData = evidenceService
            .createEvidence(hashValue, weIdWithSetAttr.getUserWeIdPrivateKey());
        Assert.assertTrue(!responseData.getResult().isEmpty());
        Assert.assertEquals(responseData.getErrorCode().intValue(), ErrorCode.SUCCESS.getCode());
        ResponseData<Boolean> responseData1 = evidenceService
            .verify(hashValue, responseData.getResult());
        logger.info("testVerifyEvidenceCase13 result :" + responseData1);
        Assert.assertEquals(responseData1.getErrorCode().intValue(),
            ErrorCode.CREDENTIAL_WEID_DOCUMENT_ILLEGAL.getCode());
        Assert.assertFalse(responseData1.getResult());
    }

    /**
     * case15: privateKey is correct ,  private is on chain(weid exist) ,but this weid not set
     * Permission.
     */
    @Test
    public void testVerifyEvidenceCase15() {
        CreateWeIdDataResult weIdWithSetAttr = weIdService.createWeId().getResult();
        String hashValue = credentialService.getCredentialHash(evidenceCredential).getResult();
        ResponseData<String> responseData1 = evidenceService
            .createEvidence(hashValue, weIdWithSetAttr.getUserWeIdPrivateKey());
        logger.info("testVerifyEvidenceCase15 result :" + responseData1);
        Assert.assertEquals(responseData1.getErrorCode().intValue(), ErrorCode.SUCCESS.getCode());
        ResponseData<Boolean> responseData2 = evidenceService
            .verify(hashValue, responseData1.getResult());
        Assert.assertEquals(responseData2.getErrorCode().intValue(),
            ErrorCode.SUCCESS.getCode());
        Assert.assertTrue(responseData2.getResult());
    }

    /**
     * case16: mock exception.
     */
    @Test
    public void testVerifyEvidenceCase16() {
        MockUp<EvidenceServiceImpl> mockException = new MockUp<EvidenceServiceImpl>() {
            @Mock
            public ResponseData<Boolean> verifySignatureToSigner(String rawData,
                String signerWeId, SignatureData signatureData) throws Exception {
                return null;
            }
        };

        Credential credential = copyCredential(evidenceCredential);
        String hashValue = credentialService.getCredentialHash(credential).getResult();
        ResponseData<Boolean> responseData = evidenceService
            .verify(hashValue, evidenceAddress);
        logger.info("testVerifyEvidenceCase16 result :" + responseData);
        Assert.assertEquals(ErrorCode.CREDENTIAL_EVIDENCE_BASE_ERROR.getCode(),
            responseData.getErrorCode().intValue());
        mockException.tearDown();
    }
}
